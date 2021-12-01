package ports

import models.User

import java.util.UUID

trait UserRepo[F[_]] {
  def create(user: User): F[User]

  def retrieve(id: UUID): F[Option[User]]

  def update(user: User): F[User]

  def delete(id: UUID): F[Unit]
}
