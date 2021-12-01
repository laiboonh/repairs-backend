package models

import cats.MonadError
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import tsec.authorization.AuthorizationInfo

import java.util.UUID

case class User(id: UUID, name: String, role: Role)

object User {
  implicit def authRole[F[_]](implicit F: MonadError[F, Throwable]): AuthorizationInfo[F, Role, User] =
    (u: User) => F.pure(u.role)

  implicit val UserEncoder: Encoder[User] = deriveEncoder[User]
}