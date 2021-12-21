import adapters.UserRepoDoobie
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.Transactor
import io.circe.Json
import models.{Role, User}
import org.http4s.UriTemplate.PathElm
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{Method, Request, Status, UriTemplate}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import routes.UserRoutes
import eu.timepit.refined.auto._

import java.util.UUID

class UserRoutesTest extends AnyWordSpec with Matchers with ForAllTestContainer {
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

  private def uri() = UriTemplate(
    path = List(PathElm("users"))
  ).toUriIfPossible.get

  private def uri(id: UUID) = UriTemplate(
    path = List(PathElm("users"), PathElm(id.toString))
  ).toUriIfPossible.get

  "GET /users/id" when {
    "given id belongs to an existing user" should {
      "return ok status with user json" in withUserRepo { userRepoSkunk =>
        val id = UUID.randomUUID()
        for {
          _ <- userRepoSkunk.create(User(id, "foo@foo.com", "password", Role.BasicUser))
          res <- UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.GET, uri = uri(id))
          )
          json <- res.as[Json]
        } yield {
          val expectedJson = Json.obj(
            ("id", Json.fromString(id.toString)),
            ("email", Json.fromString("foo@foo.com")),
            ("password", Json.fromString("password")),
            ("role", Json.fromString("BasicUser"))
          )
          res.status shouldBe Status.Ok
          json shouldBe expectedJson
        }
      }
    }
    "given id of a non existing user" should {
      "return notfound status with no payload" in withUserRepo { userRepoSkunk =>
        for {
          res <- routes.UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.GET, uri = uri(UUID.randomUUID()))
          )
          payload <- res.body.compile.toVector
        } yield {
          res.status shouldBe Status.NotFound
          payload shouldBe empty
        }
      }
    }
  }

  "DELETE /users/id" when {
    "given id belongs to an existing user" should {
      "return ok status with no payload and user removed from database" in withUserRepo { userRepoSkunk =>
        val id = UUID.randomUUID()
        for {
          _ <- userRepoSkunk.create(User(id, "foo@foo.com", "password", Role.BasicUser))
          res <- UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.DELETE, uri = uri(id))
          )
          payload <- res.body.compile.toVector
          retrieval <- userRepoSkunk.retrieve(id)
        } yield {
          retrieval shouldBe None
          res.status shouldBe Status.Ok
          payload shouldBe empty
        }
      }
    }
    "given id of a non existing user" should {
      "return InternalServerError status with no payload" in withUserRepo { userRepoSkunk =>
        for {
          res <- routes.UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.DELETE, uri = uri(UUID.randomUUID()))
          )
          payload <- res.as[String]
        } yield {
          res.status shouldBe Status.InternalServerError
          payload shouldBe "Unexpected 0 number of rows affected"
        }
      }
    }
  }

  "PUT /users/id" when {
    "given id belongs to an existing user" should {
      "return ok status with no payload and user updated" in withUserRepo { userRepoSkunk =>
        val id = UUID.randomUUID()
        val createdUser = User(id, "foo@foo.com", "password", Role.BasicUser)
        val updatedUser = createdUser.copy(email = "foo@bar.com")
        for {
          _ <- userRepoSkunk.create(createdUser)
          res <- UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.PUT, uri = uri(id)).withEntity(updatedUser)
          )
          payload <- res.body.compile.toVector
          retrieval <- userRepoSkunk.retrieve(id)
        } yield {
          retrieval shouldBe Option(updatedUser)
          res.status shouldBe Status.Ok
          payload shouldBe empty
        }
      }
    }
    "given id of a non existing user" should {
      "return notfound status with no payload" in withUserRepo { userRepoSkunk =>
        val id = UUID.randomUUID()
        val nonExistingUser = User(id, "foo@foo.com", "password", Role.BasicUser)
        for {
          res <- routes.UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.PUT, uri = uri(UUID.randomUUID())).withEntity(nonExistingUser)
          )
          payload <- res.body.compile.toVector
        } yield {
          res.status shouldBe Status.NotFound
          payload shouldBe empty
        }
      }
    }
  }

  "POST /users" when {
    "given proper user details" should {
      "return ok status with no payload and user created" in withUserRepo { userRepoSkunk =>
        val id = UUID.randomUUID()
        val createdUser = User(id, "foo@foo.com", "password", Role.BasicUser)
        for {
          res <- UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.POST, uri = uri()).withEntity(createdUser)
          )
          payload <- res.body.compile.toVector
          retrieval <- userRepoSkunk.retrieve(id)
        } yield {
          retrieval shouldBe Option(createdUser)
          res.status shouldBe Status.Ok
          payload shouldBe empty
        }
      }
    }
    "given details of an existing user" should {
      "return internalServerError status with no payload" in withUserRepo { userRepoSkunk =>
        val id = UUID.randomUUID()
        val existingUser = User(id, "foo@foo.com", "password", Role.BasicUser)
        for {
          _ <- userRepoSkunk.create(existingUser)
          res <- routes.UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.POST, uri = uri()).withEntity(existingUser)
          )
          payload <- res.body.compile.toVector
        } yield {
          res.status shouldBe Status.InternalServerError
          payload shouldBe empty
        }
      }
    }
  }
}
