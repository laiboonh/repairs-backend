package ports

import models.User

import java.util.UUID

trait UserRepo[F[_]] {
  def create(user: User): F[Either[String, User]]

  def retrieve(id: UUID): F[Option[User]]

  def update(user: User): F[Either[String, Unit]]

  def delete(id: UUID): F[Either[String, Unit]]
}
