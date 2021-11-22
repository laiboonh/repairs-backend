import cats.effect.IO
import io.circe.Json
import models.User
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{Method, Request, Response, Status}
import ports.UserRepo
import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserRoutesTest extends AnyWordSpec with Matchers {
  private val some: UserRepo[IO] = new UserRepo[IO] {
    override def find(userId: String): IO[Option[User]] = IO.pure(Some(User("johndoe@gmail.com", "john doe")))

    override def insert(user: User): IO[Unit] = IO.raiseError(new RuntimeException("should not invoke this"))
  }
  private val none: UserRepo[IO] = new UserRepo[IO] {
    override def find(userId: String): IO[Option[User]] = IO.pure(None)

    override def insert(user: User): IO[Unit] = IO.raiseError(new RuntimeException("should not invoke this"))
  }

  "GET /user/not-used" when {
    "ports.UserRepo.find returns some response" should {
      "return ok status with user json" in {
        val response: IO[Response[IO]] = UserRoutes[IO](some).orNotFound.run(
          Request(method = Method.GET, uri = uri"/user/not-used")
        )
        val expectedJson = Json.obj(
          ("email", Json.fromString("johndoe@gmail.com")),
          ("name", Json.fromString("john doe"))
        )
        val actualResponse = response.unsafeRunSync()
        actualResponse.status shouldBe Status.Ok
        actualResponse.as[Json].unsafeRunSync() shouldBe expectedJson
      }
    }
    "ports.UserRepo.find returns notFound response" should {
      "return ok status with user json" in {
        val response: IO[Response[IO]] = UserRoutes[IO](none).orNotFound.run(
          Request(method = Method.GET, uri = uri"/user/not-used")
        )

        val actualResponse = response.unsafeRunSync()
        actualResponse.status shouldBe Status.NotFound
        actualResponse.body.compile.toVector.unsafeRunSync.isEmpty shouldBe true
      }
    }

  }
}
