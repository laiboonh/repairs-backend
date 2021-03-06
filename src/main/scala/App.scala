import adapters.UserRepoDoobie
import auth.AuthHelper
import cats._
import cats.effect._
import cats.effect.std.Console
import cats.implicits._
import conf.AppConfig
import eu.timepit.refined.auto._
import fs2.io.net.Network
import io.circe.generic.auto._
import io.circe.syntax._
import models.User
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.dsl.impl._
import org.http4s.headers._
import org.http4s.implicits._
import ports.UserRepo
import routes.{LoginRoutes, UserRoutes}
import tsec.authentication._
import tsec.mac.jca.HMACSHA256

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

  def allRoutes[F[_] : Concurrent : Network : Console](authHelper: AuthHelper[F])(userRepo: UserRepo[F]): HttpApp[F] = {
    import org.http4s.server.middleware._
    CORS.policy.withAllowOriginAll(
      movieRoutes[F] <+>
        directorRoutes[F] <+>
        UserRoutes(userRepo) <+>
        new LoginRoutes(userRepo).routes(authHelper.jwtStatefulAuth) <+>
        test(authHelper)
    ).orNotFound
  }

  def test[F[_] : Concurrent](authHelper: AuthHelper[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    authHelper.auth.liftService(TSecAuthService {
      //Where user is the case class User above
      case request@GET -> Root / "test" asAuthed user =>
        /*
        Note: The request is of type: SecuredRequest, which carries:
        1. The request
        2. The Authenticator (i.e token)
        3. The identity (i.e in this case, User)
         */
        val r: SecuredRequest[F, User, AugmentedJWT[HMACSHA256, UUID]] = request
        Ok()
    })
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      appConfig <- AppConfig.config.load[IO]
      transactor = appConfig.databaseConfig.doobieTransactor[IO]
      userRepo = new UserRepoDoobie[IO](transactor)
      authHelper = new AuthHelper[IO](appConfig, userRepo)
      exitCode <- BlazeServerBuilder[IO]
        .bindHttp(appConfig.apiConfig.port, "0.0.0.0")
        .withHttpApp(allRoutes[IO](authHelper)(userRepo))
        .resource
        .use(_ => IO.never)
        .as(ExitCode.Success)
    } yield exitCode
}

