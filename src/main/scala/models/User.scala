package models

import cats.effect.kernel.Concurrent
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

import java.util.UUID

case class User(id: UUID, name: String, role: Role)

object User {
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]

  implicit val userDecoder: Decoder[User] = deriveDecoder[User]

  implicit def userEncoderF[F[_]]: EntityEncoder[F, User] = jsonEncoderOf[F, User]

  implicit def userDecoderF[F[_] : Concurrent]: EntityDecoder[F, User] = jsonOf[F, User]
}


