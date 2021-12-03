package routes

import cats.effect.kernel.Concurrent
import cats.implicits._
import io.circe.generic.auto._
import models.{LoginDetails, User}
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import ports.UserRepo
import tsec.authentication.JWTAuthenticator
import tsec.mac.jca.HMACSHA256

import java.util.UUID

class LoginRoutes[F[_] : Concurrent](userRepo: UserRepo[F]) {

  def verifyLogin(id: UUID): F[Either[String, User]] = userRepo.retrieve(id) map {
    case Some(value) => Right(value)
    case None => Left(s"User $id not found")
  }

  def routes(authenticator: JWTAuthenticator[F, UUID, User, HMACSHA256]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    implicit val loginDetailsDecoder: EntityDecoder[F, LoginDetails] = jsonOf[F, LoginDetails]

    HttpRoutes.of[F] {
      case req@POST -> Root / "login" =>
        for {
          loginDetails <- req.as[LoginDetails]
          userEither <- verifyLogin(loginDetails.id)
          res <- userEither match {
            case Left(error) => Forbidden(error)
            case Right(user) =>
              authenticator.create(user.id).map(authenticator.embed(Response(Status.Ok), _))
          }
        } yield res
    }
  }
}
