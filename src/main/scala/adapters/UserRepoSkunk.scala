package adapters


import cats.effect._
import cats.implicits.toFunctorOps
import models.User
import ports.UserRepo
import skunk._
import skunk.codec.all._
import skunk.data.Completion
import skunk.implicits._

class UserRepoSkunk[F[_] : Concurrent](val session: Resource[F, Session[F]]) extends UserRepo[F] {

  private val findOne: Query[String, User] =
    sql"select email, name from users where email = $name".query(varchar ~ varchar).gmap[User]

  override def find(email: String): F[Option[User]] = session.use { s =>
    s.prepare(findOne).use { ps =>
      ps.option(email)
    }
  }

  private val insertOne: Command[User] =
    sql"INSERT INTO users VALUES ($varchar, $varchar)"
      .command
      .gcontramap[User]

  override def insert(user: User): F[Unit] = session.use { s =>
    s.prepare(insertOne).use(_.execute(user)).void
  }

  def createTable: F[Completion] = session.use { s =>
    val command =
      sql"""
        CREATE TABLE IF NOT EXISTS users (
          "email" varchar NOT NULL,
          "name" varchar NOT NULL,
          CONSTRAINT users_pk PRIMARY KEY (email)
        )
        """.command
    s.execute(command)
  }
}
