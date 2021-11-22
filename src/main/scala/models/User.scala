package models

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class User(email: String, name: String)

object User {
  implicit val UserEncoder: Encoder[User] = deriveEncoder[User]
}