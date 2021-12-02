import adapters.UserRepoSkunk
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import io.circe.Json
import models.{Role, User}
import natchez.Trace.Implicits.noop
import org.http4s.UriTemplate.PathElm
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{Method, Request, Status, UriTemplate}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import routes.UserRoutes
import skunk.{Session, Strategy}

import java.util.UUID

class UserRoutesTest extends AnyWordSpec with Matchers with ForAllTestContainer {
  override val container: PostgreSQLContainer = PostgreSQLContainer()

  def withUserRepo(testCode: UserRepoSkunk[IO] => IO[Assertion]): Assertion = {
    val userRepoSkunk = new UserRepoSkunk[IO](Session.single(
      host = container.host,
      port = container.mappedPort(5432),
      user = container.username,
      database = container.databaseName,
      password = Some(container.password),
      strategy = Strategy.SearchPath
    ))
    (for {
      _ <- userRepoSkunk.session.use(_.execute(UserRepoSkunk.createRoleEnum))
      _ <- userRepoSkunk.session.use(_.execute(UserRepoSkunk.createTable))
      assertion <- testCode(userRepoSkunk)
      _ <- userRepoSkunk.session.use { s =>
        s.transaction.use { _ =>
          for {
            _ <- s.execute(UserRepoSkunk.dropTable)
            res <- s.execute(UserRepoSkunk.dropEnum)
          } yield res
        }
      }
    } yield assertion).unsafeRunSync()
  }

  private def uri(id: UUID) = UriTemplate(
    path = List(PathElm("users"), PathElm(id.toString))
  ).toUriIfPossible.get

  "GET /users/id" when {
    "given id belongs to an existing user" should {
      "return ok status with user json" in withUserRepo { userRepoSkunk =>
        val id = UUID.randomUUID()
        for {
          _ <- userRepoSkunk.create(User(id, "foo", Role.BasicUser))
          res <- UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.GET, uri = uri(id))
          )
          json <- res.as[Json]
        } yield {
          val expectedJson = Json.obj(
            ("id", Json.fromString(id.toString)),
            ("name", Json.fromString("foo")),
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
          _ <- userRepoSkunk.create(User(id, "foo", Role.BasicUser))
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
      "return notfound status with no payload" in withUserRepo { userRepoSkunk =>
        for {
          res <- routes.UserRoutes(userRepoSkunk).orNotFound.run(
            Request(method = Method.DELETE, uri = uri(UUID.randomUUID()))
          )
          payload <- res.body.compile.toVector
        } yield {
          res.status shouldBe Status.NotFound
          payload shouldBe empty
        }
      }
    }
  }
}
