import adapters.UserRepoDoobie
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.Transactor
import models.{Role, User}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class UserRepoDoobieTest extends AnyWordSpec with Matchers with ForAllTestContainer {
  override val container: PostgreSQLContainer = PostgreSQLContainer()

  def withUserRepo(testCode: UserRepoDoobie[IO] => IO[Assertion]): Assertion = {
    val transactor = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://${container.host}:${container.mappedPort(5432)}/${container.databaseName}",
      user = container.username,
      pass = container.password
    )
    val userRepoDoobie: UserRepoDoobie[IO] = new UserRepoDoobie(transactor)
    (for {
      _ <- userRepoDoobie.DDL.createRoleEnum
      _ <- userRepoDoobie.DDL.createTable
      assertion <- testCode(userRepoDoobie)
      _ <- userRepoDoobie.DDL.dropTable
      _ <- userRepoDoobie.DDL.dropEnum
    } yield assertion).unsafeRunSync()
  }

  "retrieve" when {
    val id = UUID.randomUUID()
    "given id of existing user" should {
      "return some user" in withUserRepo { userRepoDoobie =>
        val newUser = User(id, "john doe", Role.BasicUser)
        for {
          _ <- userRepoDoobie.create(newUser)
          maybeUser <- userRepoDoobie.retrieve(id)
        } yield maybeUser shouldBe Some(newUser)
      }
    }
    "given id of non existing user" should {
      "return none" in withUserRepo { userRepoDoobie =>
        userRepoDoobie.retrieve(id).map(_ shouldBe None)
      }
    }
  }

  "delete" when {
    val id = UUID.randomUUID()
    "given id of existing user" should {
      "return ()" in withUserRepo { userRepoDoobie =>
        val newUser = User(id, "john doe", Role.BasicUser)
        for {
          _ <- userRepoDoobie.create(newUser)
          result <- userRepoDoobie.delete(id)
        } yield result shouldBe Right(())
      }
    }
    "given id of non existing user" should {
      "return error message" in withUserRepo { userRepoDoobie =>
        userRepoDoobie.delete(id).map(_ shouldBe Left("Unexpected 0 number of rows affected"))
      }
    }
  }

  "update" when {
    "given an existing user" should {
      "return ()" in withUserRepo { userRepoDoobie =>
        val id = UUID.randomUUID()
        val newUser = User(id, "john doe", Role.BasicUser)
        for {
          _ <- userRepoDoobie.create(newUser)
          updatedUser = newUser.copy(name = "foo", role = Role.Administrator)
          result <- userRepoDoobie.update(updatedUser)
        } yield result shouldBe Right(())
      }
    }
    "given a non existing user" should {
      "will return error message" in withUserRepo { userRepoDoobie =>
        val id = UUID.randomUUID()
        val newUser = User(id, "john doe", Role.BasicUser)
        for {
          result <- userRepoDoobie.update(newUser)
        } yield result shouldBe Left("Unexpected 0 number of rows affected")
      }
    }
  }

  "create" when {
    "given an existing user" should {
      "return error message" in withUserRepo { userRepoDoobie =>
        val id = UUID.randomUUID()
        val newUser = User(id, "john doe", Role.BasicUser)
        for {
          _ <- userRepoDoobie.create(newUser)
          result <- userRepoDoobie.create(newUser)
        } yield result shouldBe Left("Unique Violation")
      }
    }
  }

}
