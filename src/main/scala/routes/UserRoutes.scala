package routes

import cats.effect.Concurrent
import cats.implicits._
import io.circe.syntax._
import models.User
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import ports.UserRepo

object UserRoutes {
  def apply[F[_]](repo: UserRepo[F])(implicit F: Concurrent[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "users" / UUIDVar(id) =>
        for {
          userOpt <- repo.retrieve(id)
          res <- userOpt match {
            case Some(user) => Ok(user.asJson)
            case None => NotFound()
          }
        } yield res
      case DELETE -> Root / "users" / UUIDVar(id) =>
        for {
          userOpt <- repo.retrieve(id)
          resF <- userOpt match {
            case Some(_) => repo.delete(id).map(_ => Ok())
            case None => F.pure(NotFound())
          }
          res <- resF
        } yield res
      case req@PUT -> Root / "users" / UUIDVar(id) =>
        for {
          userOpt <- repo.retrieve(id)
          user <- req.as[User]
          resF <- userOpt match {
            case Some(_) => repo.update(user).map(_ => Ok())
            case None => F.pure(NotFound())
          }
          res <- resF
        } yield res
      case req@POST -> Root / "users" =>
        for {
          user <- req.as[User]
          resF <- repo.create(user).map {
            case Left(_) => InternalServerError()
            case Right(_) => Ok()
          }
          res <- resF
        } yield res
    }
  }
}
