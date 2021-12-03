package ports

import cats.ApplicativeError
import models.User

import java.util.UUID

trait UserRepo[F[_]] {
  def create(user: User)(implicit ae: ApplicativeError[F, Throwable]): F[Either[String, User]]

  def retrieve(id: UUID): F[Option[User]]

  def update(user: User): F[User]

  def delete(id: UUID): F[Unit]
}
