import App.dbCredEitherConfig
import adapters.UserRepoSkunk
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import skunk.data.Completion

object AdminTasks {
  val databaseCredIO: IO[App.DatabaseCredentials] = for {
    databaseCredEither <- dbCredEitherConfig.load[IO]
    databaseCredIO <- IO.fromEither(databaseCredEither.left.map(new RuntimeException(_)))
  } yield databaseCredIO

  val userRepoIO: IO[UserRepoSkunk[IO]] = for {
    databaseCred <- databaseCredIO
  } yield new UserRepoSkunk(App.skunkSession[IO](databaseCred))

  def run: Completion = userRepoIO.flatMap(userRepo => userRepo.createTable).unsafeRunSync()
}
