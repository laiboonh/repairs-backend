import adapters.UserRepoSkunk
import cats._
import cats.effect._
import cats.effect.std.Console
import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.NonSystemPortNumber
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

  def session[F[_] : Concurrent : Network : Console]: Resource[F, Session[F]] = Session.single(
    host = "localhost",
    port = 5432,
    user = "postgres",
    database = "test",
    password = Some("1234")
  )

  def allRoutes[F[_] : Concurrent : Network : Console]: HttpRoutes[F] = movieRoutes[F] <+> directorRoutes[F] <+> UserRoutes.apply(new UserRepoSkunk(session))

  def allRoutesComplete[F[_] : Concurrent : Network : Console]: HttpApp[F] = allRoutes[F].orNotFound

  def port: ConfigValue[Effect, NonSystemPortNumber] = env("PORT").as[NonSystemPortNumber].default(8080)

  private val apis: HttpApp[IO] = Router("/api" -> App.allRoutes[IO]).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    port.load[IO].flatMap(
      BlazeServerBuilder[IO]
        .bindHttp(_, "0.0.0.0")
        .withHttpApp(apis)
        .resource
        .use(_ => IO.never)
        .as(ExitCode.Success))
}

