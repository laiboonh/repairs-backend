package ports

import models.User

trait UserRepo[F[_]] {
  def find(email: String): F[Option[User]]

  def insert(user: User): F[Unit]
}
