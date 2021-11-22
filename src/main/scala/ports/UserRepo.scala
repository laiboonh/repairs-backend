package ports

import models.User

trait UserRepo[F[_]] {
  def find(userId: String): F[Option[User]]

  def insert(user: User): F[Unit]
}
