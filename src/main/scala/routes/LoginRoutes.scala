package routes

import cats.effect.kernel.Concurrent
import cats.implicits._
import io.circe.generic.auto._
import models.User
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import org.reactormonk.{CryptoBits, PrivateKey}
import ports.UserRepo

import java.time._

class LoginRoutes[F[_] : Concurrent](userRepo: UserRepo[F]) {

  private val key = PrivateKey(scala.io.Codec.toUTF8(scala.util.Random.alphanumeric.take(20).mkString("")))
  private val crypto = CryptoBits(key)
  private val clock = Clock.systemUTC

  def verifyLogin(email: String): F[Either[String, User]] = userRepo.find(email) map {
    case Some(value) => Right(value)
    case None => Left("user not found")
  }

  case class LoginDetails(email: String)

  val routes: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    implicit val loginDetailsDecoder: EntityDecoder[F, LoginDetails] = jsonOf[F, LoginDetails]

    HttpRoutes.of[F] {
      case req@POST -> Root / "login" =>
        for {
          loginDetails <- req.as[LoginDetails]
          userEither <- verifyLogin(loginDetails.email)
          res <- userEither match {
            case Left(error) => Forbidden(error)
            case Right(value) =>
              val message = crypto.signToken(value.email, clock.millis.toString)
              Ok(s"Welcome back! ${value.name}").map(_.addCookie(ResponseCookie("authcookie", message)))
          }
        } yield res
    }
  }
}
