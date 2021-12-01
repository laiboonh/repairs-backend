package admin

import adapters.UserRepoSkunk
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import conf.DatabaseConfig
import skunk.data.Completion

object Tasks {

  val userRepoIO: IO[UserRepoSkunk[IO]] = for {
    databaseConfig <- DatabaseConfig.config.load[IO]
  } yield new UserRepoSkunk(databaseConfig.skunkSession[IO])

  def run: Completion = (for {
    userRepo <- userRepoIO
    _ <- userRepo.session.use(_.execute(UserRepoSkunk.createRoleEnum))
    result <- userRepo.session.use(_.execute(UserRepoSkunk.createTable))
  } yield result).unsafeRunSync()


}
