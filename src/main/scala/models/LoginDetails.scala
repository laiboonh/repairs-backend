package models

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import java.util.UUID

case class LoginDetails(id: UUID)

object LoginDetails {
  implicit val LoginDetailsEncoder: Encoder[LoginDetails] = deriveEncoder[LoginDetails]
}