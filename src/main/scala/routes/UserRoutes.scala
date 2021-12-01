package routes

import cats.effect.Concurrent
import cats.implicits._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import ports.UserRepo

object UserRoutes {
  def apply[F[_] : Concurrent](repo: UserRepo[F]): HttpRoutes[F] = {
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
      //TODO CRUD with tests

    }
  }
}
