import adapters.UserRepoSkunk
import auth.AuthHelper
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import conf.{ApiConfig, AppConfig, DatabaseConfig}
import eu.timepit.refined.auto._
import models.{LoginDetails, Role, User}
import natchez.Trace.Implicits.noop
import org.http4s.circe._
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.{EntityEncoder, Method, Request, Status}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import routes.LoginRoutes
import skunk.{Session, Strategy}

import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps


class LoginRoutesTest extends AnyWordSpec with Matchers with ForAllTestContainer {
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

  private val testAppConfig: AppConfig = AppConfig(
    ApiConfig(1234, 1 minutes),
    DatabaseConfig("", "", "0.0.0.0", 1234, "")
  )

  "POST /login/" when {
    "given existing id" should {
      "return token in header" in withUserRepo { userRepoSkunk =>
        val id = UUID.randomUUID()
        val body = LoginDetails(id)
        for {
          _ <- userRepoSkunk.create(User(id, "foo", Role.BasicUser))
          authHelper = new AuthHelper(testAppConfig, userRepoSkunk)
          res <- new LoginRoutes(userRepoSkunk).routes(authHelper.jwtStatefulAuth).orNotFound.run(
            Request(method = Method.POST, uri = uri"/login").withEntity(body)
          )
        } yield {
          res.status shouldBe Status.Ok
          res.headers.get[Authorization] shouldBe defined
        }
      }
    }
  }
}
