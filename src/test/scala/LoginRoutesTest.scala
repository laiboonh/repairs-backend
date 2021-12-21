import adapters.UserRepoDoobie
import auth.AuthHelper
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import conf.{ApiConfig, AppConfig, DatabaseConfig}
import doobie.Transactor
import eu.timepit.refined.auto._
import models.{LoginDetails, Role, User}
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.{Method, Request, Status}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import routes.LoginRoutes

import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps


class LoginRoutesTest extends AnyWordSpec with Matchers with ForAllTestContainer {
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
          _ <- userRepoSkunk.create(User(id, "foo@foo.com", "bar", Role.BasicUser))
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
