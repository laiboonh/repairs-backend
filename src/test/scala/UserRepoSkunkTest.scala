import adapters.UserRepoSkunk
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import models.User
import natchez.Trace.Implicits.noop
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import skunk.Session
import skunk.data.Completion
import skunk.implicits.toStringOps

class UserRepoSkunkTest extends AnyWordSpec with Matchers with ForAllTestContainer {
  override val container: PostgreSQLContainer = PostgreSQLContainer()

  def withUserRepo(testCode: UserRepoSkunk[IO] => IO[Assertion]): Assertion = {
    val userRepoSkunk = new UserRepoSkunk[IO](Session.single(
      host = container.host,
      port = container.mappedPort(5432),
      user = container.username,
      database = container.databaseName,
      password = Some(container.password)
    ))
    (for {
      _ <- userRepoSkunk.createTable
      assertion <- testCode(userRepoSkunk)
      _ <- truncateTable(userRepoSkunk, "users")
    } yield assertion).unsafeRunSync()
  }

  def truncateTable(userRepo: UserRepoSkunk[IO], tableName: String): IO[Completion] = {
    val command = sql"truncate #$tableName".command
    userRepo.session.use { s =>
      s.execute(command)
    }
  }

  "Skunk.find" when {
    "given email of existing user" should {
      "return some user" in withUserRepo { userRepoSkunk =>
        val newUser = User("johndoe@gmail.com", "john doe")
        for {
          _ <- userRepoSkunk.insert(newUser)
          maybeUser <- userRepoSkunk.find("johndoe@gmail.com")
        } yield maybeUser shouldBe Some(newUser)
      }
    }
    "given email of non existing user" should {
      "return none" in withUserRepo { userRepoSkunk =>
        userRepoSkunk.find("alice@gmail.com").map(_ shouldBe None)
      }
    }
  }

}
