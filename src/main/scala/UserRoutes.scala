import cats.effect.Concurrent
import cats.effect.std.Console
import cats.implicits._
import fs2.io.net.Network
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response, Status}
import ports.UserRepo

object UserRoutes {
  def apply[F[_] : Concurrent : Network : Console](repo: UserRepo[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "users" / id =>
        repo.find(id).map {
          case Some(user) => Response(status = Status.Ok).withEntity(user.asJson)
          case None => Response(status = Status.NotFound)
        }
    }
  }
}
