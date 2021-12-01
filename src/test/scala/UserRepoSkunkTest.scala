import adapters.UserRepoSkunk
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import models.{Role, User}
import natchez.Trace.Implicits.noop
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import skunk.{Session, Strategy}

import java.util.UUID

class UserRepoSkunkTest extends AnyWordSpec with Matchers with ForAllTestContainer {
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

  "Skunk.retrieve" when {
    val id = UUID.randomUUID()
    "given id of existing user" should {
      "return some user" in withUserRepo { userRepoSkunk =>
        val newUser = User(id, "john doe", Role("BasicUser"))
        for {
          _ <- userRepoSkunk.create(newUser)
          maybeUser <- userRepoSkunk.retrieve(id)
        } yield maybeUser shouldBe Some(newUser)
      }
    }
    "given id of non existing user" should {
      "return none" in withUserRepo { userRepoSkunk =>
        userRepoSkunk.retrieve(id).map(_ shouldBe None)
      }
    }
  }

}
