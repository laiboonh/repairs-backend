import App.dbCredEitherConfig
import adapters.UserRepoSkunk
import cats.effect.IO
import cats.effect.unsafe.implicits.global

object AdminTasks {

  private val task: IO[String] = {
    for {
      databaseCredEither <- dbCredEitherConfig.load[IO]
      result = databaseCredEither match {
        case Left(error) => error
        case Right(databaseCred) =>
          val userRepo = new UserRepoSkunk(App.session[IO](databaseCred))
          userRepo.createTable
          "Success"
      }
    } yield result
  }

  def run(): Unit = task.map(print).unsafeRunSync()
}
