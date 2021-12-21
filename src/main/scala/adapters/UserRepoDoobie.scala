package adapters

import cats.effect.Async
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import models.User
import ports.UserRepo

import java.util.UUID

class UserRepoDoobie[F[_] : Async](transactor: Transactor.Aux[F, Unit]) extends UserRepo[F] {
  override def create(user: User): F[Either[String, User]] = {
    sql"INSERT INTO users (id, email, password, role) VALUES (${user.id}, ${user.email.value}, ${user.password.value}, ${user.role})"
      .update
      .withUniqueGeneratedKeys[User]("id", "email", "password", "role")
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => "Unique Violation"
      }.transact(transactor)
  }

  override def retrieve(id: UUID): F[Option[User]] =
    sql"SELECT id, email, password, role::role from users where id = $id"
      .query[User]
      .option
      .transact(transactor)


  override def update(user: User): F[Either[String, Unit]] =
    sql"UPDATE users SET email = ${user.email.value}, password = ${user.password.value}, role = ${user.role} where id = ${user.id}"
      .update
      .run
      .map {
        case 1 => Right(())
        case n => Left(s"Unexpected $n number of rows affected")
      }
      .transact(transactor)

  override def delete(id: UUID): F[Either[String, Unit]] =
    sql"DELETE FROM users WHERE id = $id"
      .update
      .run
      .map {
        case 1 => Right(())
        case n => Left(s"Unexpected $n number of rows affected")
      }
      .transact(transactor)

  object DDL {
    def createTable: F[Int] =
      sql"""
        CREATE TABLE IF NOT EXISTS users (
          "id" uuid NOT NULL,
          "email" varchar NOT NULL,
          "password" varchar NOT NULL,
          "role" role NOT NULL,
          CONSTRAINT users_pk PRIMARY KEY (id)
        )
        """.update.run.transact(transactor)

    def dropTable: F[Int] =
      sql"""
         DROP TABLE users
       """.update.run.transact(transactor)

    def createRoleEnum: F[Int] =
      sql"""
         CREATE TYPE role AS ENUM ('Administrator', 'BasicUser')
       """.update.run.transact(transactor)

    def dropEnum: F[Int] =
      sql"""
         DROP TYPE role
       """.update.run.transact(transactor)
  }

}


