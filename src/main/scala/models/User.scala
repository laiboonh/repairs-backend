package models

import cats.MonadError
import cats.effect.kernel.Concurrent
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import tsec.authorization.AuthorizationInfo

import java.util.UUID

case class User(id: UUID, name: String, role: Role)

object User {
  implicit def authRole[F[_]](implicit F: MonadError[F, Throwable]): AuthorizationInfo[F, Role, User] =
    (u: User) => F.pure(u.role)

  implicit val userEncoder: Encoder[User] = deriveEncoder[User]

  implicit val userDecoder: Decoder[User] = deriveDecoder[User]

  implicit def userEncoderF[F[_]]: EntityEncoder[F, User] = jsonEncoderOf[F, User]

  implicit def userDecoderF[F[_] : Concurrent]: EntityDecoder[F, User] = jsonOf[F, User]

  val empty: User = User(UUID.randomUUID(), "", Role.BasicUser)
}