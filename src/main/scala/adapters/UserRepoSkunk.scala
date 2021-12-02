package adapters

import cats.effect._
import cats.implicits.toFunctorOps
import models.{Role, User}
import ports.UserRepo
import skunk._
import skunk.codec.all._
import skunk.implicits._

import java.util.UUID

class UserRepoSkunk[F[_] : Concurrent](val session: Resource[F, Session[F]]) extends UserRepo[F] {

  val userDecoder: Decoder[User] =
    (uuid ~ varchar ~ Role.codec).map { case id ~ name ~ role => User(id, name, role) }

  val userEncoder: Encoder[User] = (uuid ~ varchar ~ Role.codec).values.contramap(
    (user: User) => user.id ~ user.name ~ user.role)

  private val retrieve: Query[UUID, User] =
    sql"select id, name, role from users where id = $uuid".query(userDecoder)

  override def retrieve(id: UUID): F[Option[User]] = session.use { s =>
    s.prepare(retrieve).use { ps =>
      ps.option(id)
    }
  }

  private val create: Command[User] =
    sql"INSERT INTO users (id, name, role) VALUES $userEncoder".command

  override def create(user: User): F[User] = session.use { s =>
    s.prepare(create).use { session =>
      session.execute(user).map(_ => user)
    }
  }

  private val update: Command[User] =
    sql"UPDATE users SET name = $varchar, role = ${Role.codec} where id = $uuid"
      .command
      .contramap {
        case User(id, name, role) => name ~ role ~ id
      }

  override def update(user: User): F[User] = session.use { s =>
    s.prepare(update).use { session =>
      session.execute(user).map(_ => user)
    }
  }

  private val delete: Command[UUID] =
    sql"DELETE FROM users WHERE id = $uuid".command

  override def delete(id: UUID): F[Unit] = session.use { s =>
    s.prepare(delete).use { session =>
      session.execute(id).void
    }
  }
}

object UserRepoSkunk {
  val createTable: Command[Void] =
    sql"""
        CREATE TABLE IF NOT EXISTS users (
          "id" uuid NOT NULL,
          "name" varchar NOT NULL,
          "role" role NOT NULL,
          CONSTRAINT users_pk PRIMARY KEY (id)
        )
        """.command

  val dropTable: Command[Void] =
    sql"""
         DROP TABLE users
       """.command

  val createRoleEnum: Command[Void] =
    sql"""
         CREATE TYPE role AS ENUM ('Administrator', 'BasicUser')
       """.command

  val dropEnum: Command[Void] =
    sql"""
         DROP TYPE role
       """.command
}
