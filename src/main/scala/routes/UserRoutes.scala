package routes

import cats.effect.Concurrent
import cats.effect.std.Console
import cats.implicits._
import fs2.io.net.Network
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import ports.UserRepo

object UserRoutes {
  def apply[F[_] : Concurrent : Network : Console](repo: UserRepo[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "users" / id =>
        for {
          res <- repo.find(id).flatMap {
            case Some(user) => Ok(user.asJson)
            case None => NotFound()
          }
        } yield res
    }
  }
}
