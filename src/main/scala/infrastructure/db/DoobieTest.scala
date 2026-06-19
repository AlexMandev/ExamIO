package infrastructure.db

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.syntax.all.*

@main
def main: Unit =
  val dbModule = DBModule(DBConfig("localhost", 5432, "examio", "postgres", "postgres", 10))

  dbModule
    .use { dbModule =>
      sql"SELECT 42".query[Int].option.transact(dbModule.dbTransactor).flatMap(IO.println)
    }
    .unsafeRunSync()
