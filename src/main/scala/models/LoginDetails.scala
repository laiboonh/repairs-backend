package models

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

import java.util.UUID

case class LoginDetails(id: UUID)

object LoginDetails {
  implicit val loginDetailsEncoder: Encoder[LoginDetails] = deriveEncoder[LoginDetails]

  implicit def loginDetailsEncoderF[F[_]]: EntityEncoder[F, LoginDetails] = jsonEncoderOf[F, LoginDetails]
}