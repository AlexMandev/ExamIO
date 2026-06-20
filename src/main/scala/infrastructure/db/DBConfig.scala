package infrastructure.db

case class DBConfig(
  host: String,
  port: Int,
  dbName: String,
  user: String,
  password: String,
  connectionPoolSize: Int,
  migrationTable: String
):
  val driverClassName = "org.postgresql.Driver"
  val url: String = s"jdbc:postgresql://${host}:${port}/${dbName}"
