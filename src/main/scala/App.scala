import App.apis
import adapters.UserRepoSkunk
import cats._
import cats.effect._
import cats.effect.std.Console
import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.refineV
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.net.{NonSystemPortNumber, UserPortNumber}
import fs2.io.net.Network
import io.circe.generic.auto._
import io.circe.syntax._
import natchez.Trace.Implicits.noop
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.dsl.impl._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.server._
import ports.UserRepo
import skunk._

import java.time.Year
import java.util.UUID
import scala.collection.mutable
import scala.util.Try


object App extends IOApp {
  type Actor = String

  case class Movie(id: String, title: String, year: Year, actors: List[String], director: String)

  case class Director(firstName: String, lastName: String) {
    override def toString: String = s"$firstName $lastName"
  }

  case class DirectorDetails(firstName: String, lastName: String, genre: String)

  //HttpRoutes[F] is Request => F[OptionResponse]]

  object DirectorQueryParamMatcher extends QueryParamDecoderMatcher[String]("director")

  implicit val yearQueryParamDecoder: QueryParamDecoder[Year] = QueryParamDecoder[Int].emap { yearInt =>
    Try(Year.of(yearInt)).toEither.leftMap(e => ParseFailure(e.getMessage, e.getMessage))
  }

  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Year]("year")

  //GET /movies?director=Zach%20Snyder&year=2021
  def movieRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "movies" :? DirectorQueryParamMatcher(director) +& YearQueryParamMatcher(maybeYear) =>
        maybeYear match {
          case Some(validateYear) => validateYear.fold(
            _ => BadRequest("badly formatted year"),
            year => Ok(findMoviesByDirector(director).filter(_.year == year).asJson)
          )
          case None => Ok(findMoviesByDirector(director).asJson)
        }

      case GET -> Root / "movies" / UUIDVar(movieId) / "actors" => findMovieById(movieId) match {
        case Some(movie) => Ok(movie.asJson)
        case None => NotFound(s"Movie $movieId not found")
      }
    }
  }


  val moviesDB: Map[String, Movie] = Map(
    "1" -> Movie("1", "xxx", Year.of(2021), List.empty, "Zach Snyder")
  )

  private def findMovieById(movieId: UUID) = moviesDB.get(movieId.toString)

  private def findMoviesByDirector(director: String): List[Movie] =
    moviesDB.values.filter(_.director == director).toList

  val directorDetailsDB: mutable.Map[Director, DirectorDetails] = mutable.Map(
    Director("Zach", "Snyder") -> DirectorDetails("Zach", "Snyder", "superhero")
  )

  object DirectorPath {
    def unapply(str: String): Option[Director] = Try {
      val tokens = str.split(" ")
      Director(tokens(0), tokens(1))
    }.toOption
  }

  def directorRoutes[F[_] : Concurrent]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    implicit val directorDetailsDecoder: EntityDecoder[F, DirectorDetails] = jsonOf[F, DirectorDetails]
    implicit val directorDecoder: EntityDecoder[F, Director] = jsonOf[F, Director]

    HttpRoutes.of[F] {
      case GET -> Root / "directors" / DirectorPath(director) => directorDetailsDB.get(director) match {
        case Some(dirDetails) => Ok.apply(dirDetails.asJson)
        case None => NotFound(s"Director $director not found")
      }
      case req@POST -> Root / "directors" =>
        for {
          dirDetails <- req.as[DirectorDetails]
          director <- req.as[Director]
          _ = directorDetailsDB.put(director, dirDetails)
          res <- Ok.headers(`Content-Encoding`(ContentCoding.gzip))
            .map(_.addCookie(ResponseCookie("My-Cookie", "value")))
        } yield res
    }
  }

  def skunkSession[F[_] : Concurrent : Network : Console](databaseCred: DatabaseCredentials): Resource[F, Session[F]] = Session.single(
    host = databaseCred.host,
    port = databaseCred.port,
    user = databaseCred.username,
    database = databaseCred.databaseName,
    password = Some(databaseCred.password),
    ssl = SSL.Trusted.withFallback(true) //fallback for dev environment with no SSL
  )

  def allRoutes[F[_] : Concurrent : Network : Console](userRepo: UserRepo[F]): HttpRoutes[F] = movieRoutes[F] <+> directorRoutes[F] <+> UserRoutes(userRepo)

  def portConfig: ConfigValue[Effect, NonSystemPortNumber] = env("PORT").as[NonSystemPortNumber].default(8080)

  type DatabaseUrl = String Refined MatchesRegex["^(.+):/{2}(.+):(.+)@(.+):(.+)/(.+)$"]
  type Host = String Refined MatchesRegex["^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])(.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]))*$"]

  def dbCredEitherConfig: ConfigValue[Effect, Either[String, DatabaseCredentials]] = env("DATABASE_URL").as[DatabaseUrl].map(DatabaseCredentials(_))

  def apis[F[_] : Concurrent : Network : Console](userRepo: UserRepo[F]): HttpApp[F] = Router("/api" -> App.allRoutes[F](userRepo)).orNotFound

  case class DatabaseCredentials(username: String, password: String, host: Host, port: UserPortNumber, databaseName: String)

  object DatabaseCredentials {
    def apply(databaseUrl: DatabaseUrl): Either[String, DatabaseCredentials] = {
      val pattern = Predef.augmentString("^(.+):/{2}(.+):(.+)@(.+):(.+)/(.+)$").r
      val pattern(_, username, password, host, port, dbName) = databaseUrl.value

      def hostEither(input: String): Either[String, Host] = refineV(input)

      def portEither(input: Int): Either[String, UserPortNumber] = refineV(input)

      for {
        refinedHost <- hostEither(host)
        parsedPort <- try Right(Integer.parseInt(port)) catch {
          case e: Exception => Left(e.getMessage)
        }
        refinedPort <- portEither(parsedPort)
      } yield DatabaseCredentials(
        username, password, refinedHost, refinedPort, dbName
      )
    }
  }

  val databaseCredIO: IO[DatabaseCredentials] = for {
    databaseCredEither <- dbCredEitherConfig.load[IO]
    databaseCredIO <- IO.fromEither(databaseCredEither.left.map(new RuntimeException(_)))
  } yield databaseCredIO

  override def run(args: List[String]): IO[ExitCode] = {

    for {
      port <- portConfig.load[IO]
      databaseCred <- databaseCredIO
      session = skunkSession[IO](databaseCred)
      userRepo = new UserRepoSkunk[IO](session)
      _ <- userRepo.createTable //bootstrap table
      exitCode <- BlazeServerBuilder[IO]
        .bindHttp(port, "0.0.0.0")
        .withHttpApp(apis[IO](userRepo))
        .resource
        .use(_ => IO.never)
        .as(ExitCode.Success)
    } yield exitCode
  }
}

