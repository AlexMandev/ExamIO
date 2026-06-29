name := "final-project"
version := "0.1"

scalaVersion := "3.8.3"

scalacOptions ++= Seq(
  "-new-syntax",
  "-Werror",
  "-deprecation"
)

val doobieVersion = "1.0.0-RC12"
val tapirVersion = "1.13.23"
val http4sVersion = "0.23.34"
val flywayVersion = "12.7.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.13.0",
  "org.typelevel" %% "cats-effect" % "3.7.0",

  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres-circe" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,

  "com.github.jwt-scala" %% "jwt-circe" % "10.0.1",

  "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-cats" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,

  "org.mindrot" % "jbcrypt" % "0.4",

  "org.http4s" %% "http4s-ember-server" % http4sVersion,

  "org.flywaydb" % "flyway-core" % flywayVersion,
  "org.flywaydb" % "flyway-database-postgresql" % flywayVersion,

  "com.typesafe" % "config" % "1.4.8",

  "ch.qos.logback" % "logback-classic" % "1.5.34",
  "org.fusesource.jansi" % "jansi" % "2.4.3",

  "org.scalatest" %% "scalatest" % "3.2.20" % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
