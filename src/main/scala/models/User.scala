package models

import cats.effect.kernel.Concurrent
import core.RefinedTypes.{Email, Password}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import io.circe.refined._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

import java.util.UUID

case class User(id: UUID, email: Email, password: Password, role: Role)

object User {
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]

  implicit val userDecoder: Decoder[User] = deriveDecoder[User]

  implicit def userEncoderF[F[_]]: EntityEncoder[F, User] = jsonEncoderOf[F, User]

  implicit def userDecoderF[F[_] : Concurrent]: EntityDecoder[F, User] = jsonOf[F, User]
}


