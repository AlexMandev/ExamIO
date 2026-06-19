package infrastructure.db

import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import infrastructure.db.DBDoobie.DBTransactor
import doobie.util.ExecutionContexts

case class DBModule(dbTransactor: DBTransactor)

object DBModule:
  def apply(dbConfig: DBConfig): Resource[IO, DBModule] = for
    execContext <- ExecutionContexts.fixedThreadPool(dbConfig.connectionPoolSize)

    transactor <- HikariTransactor.newHikariTransactor(
      dbConfig.driverClassName,
      dbConfig.url,
      dbConfig.user,
      dbConfig.password,
      execContext
    )
  yield DBModule(transactor)
