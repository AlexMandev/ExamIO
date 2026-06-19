package infrastructure.db

import cats.effect.IO
import doobie.hikari.HikariTransactor

object DBDoobie:
  type DBTransactor = HikariTransactor[IO]
