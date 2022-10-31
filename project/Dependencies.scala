import sbt.*

object dependencies {

  object main {

    private val cirisV  = "2.3.3"
    private val circeV  = "0.14.2"
    private val doobieV = "1.0.0-RC2"
    private val http4sV = "0.23.13"
    private val tapirV  = "1.0.1"

    val cats       = "org.typelevel" %% "cats-core"   % "2.8.0"
    val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.12"

    val ciris = "is.cir" %% "ciris" % cirisV

    val circe        = "io.circe"             %% "circe-core"    % circeV
    val circeGeneric = "io.circe"             %% "circe-generic" % circeV
    val circeLiteral = "io.circe"             %% "circe-literal" % circeV % Test
    val jwtCirce     = "com.github.jwt-scala" %% "jwt-circe"     % "9.0.5"

    val http4sEmberServer = "org.http4s" %% "http4s-ember-server" % http4sV
    val http4sEmberClient = "org.http4s" %% "http4s-ember-client" % http4sV
    val http4sDsl         = "org.http4s" %% "http4s-dsl"          % http4sV
    val http4sCirce       = "org.http4s" %% "http4s-circe"        % http4sV

    val tapir          = "com.softwaremill.sttp.tapir" %% "tapir-core"              % tapirV
    val tapirHttp4s    = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirV
    val tapirJsonCirce = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirV
    val tapirSwagger   = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirV

    val doobie              = "org.tpolecat" %% "doobie-core"           % doobieV
    val doobieHikari        = "org.tpolecat" %% "doobie-hikari"         % doobieV
    val doobiePostgres      = "org.tpolecat" %% "doobie-postgres"       % doobieV
    val doobiePostgresCirce = "org.tpolecat" %% "doobie-postgres-circe" % doobieV

    val flyway = "org.flywaydb" % "flyway-core" % "8.5.12"

    val logback       = "ch.qos.logback"      % "logback-classic" % "1.2.11"
    val janino        = "org.codehaus.janino" % "janino"          % "3.1.7"
    val log4catsSlf4j = "org.typelevel"      %% "log4cats-slf4j"  % "2.3.1"

    val bouncyCastle = "org.bouncycastle" % "bcprov-jdk15on" % "1.70"

    val identityClient = "io.blindnet" %% "identity-client" % "1.0.1-SNAPSHOT"
  }

  object test {
    private val testContainersV = "0.40.10"

    val scalaCheck         = "org.scalacheck" %% "scalacheck"                    % "1.16.0" % Test
    val ceTestingScalatest = "org.typelevel"  %% "cats-effect-testing-scalatest" % "1.4.0"  % Test

    val weaver = "com.disneystreaming" %% "weaver-cats" % "0.7.15" % Test

    val testContainers = "com.dimafeng" %% "testcontainers-scala-scalatest" % testContainersV % Test
    val testContainersPosgres =
      "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersV % Test

  }

}
