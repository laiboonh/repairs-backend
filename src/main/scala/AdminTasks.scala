import adapters.UserRepoSkunk
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import skunk.data.Completion

object AdminTasks {

  val userRepoIO: IO[UserRepoSkunk[IO]] = for {
    databaseConfig <- DatabaseConfig.config.load[IO]
  } yield new UserRepoSkunk(databaseConfig.skunkSession[IO])

  def run: Completion = userRepoIO.flatMap(userRepo => userRepo.createTable).unsafeRunSync()
}
