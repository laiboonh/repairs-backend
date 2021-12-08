package admin

import adapters.UserRepoDoobie
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import conf.DatabaseConfig

object Tasks {
  val userRepoDoobieIO: IO[UserRepoDoobie[IO]] = for {
    databaseConfig <- DatabaseConfig.config.load[IO]
  } yield new UserRepoDoobie(databaseConfig.doobieTransactor[IO])

  def run = (for {
    userRepoDoobie <- userRepoDoobieIO
    result <- userRepoDoobie.DDL.dropTable
    //    result <- userRepoDoobie.DDL.createTable
  } yield result).map(println(_)).unsafeRunSync()


}
