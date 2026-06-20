package infrastructure.db

import cats.effect.IO
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{CleanResult, MigrateResult}

class DBMigrator(dbConfig: DBConfig):
  private val flyway = Flyway
    .configure()
    .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
    .locations("classpath:/db-migrations")
    .table(dbConfig.migrationTable)
    .load()

  def migrate: IO[MigrateResult] =
    IO.blocking(flyway.migrate())

  def clean: IO[CleanResult] = IO.blocking(flyway.clean())
