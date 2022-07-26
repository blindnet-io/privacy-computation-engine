import dependencies.*

ThisBuild / scalaVersion                                   := "3.1.3"
ThisBuild / version                                        := "0.2.0-SNAPSHOT"
ThisBuild / organization                                   := "io.blindnet"
ThisBuild / organizationName                               := "blindnet"
ThisBuild / organizationHomepage                           := Some(url("https://blindnet.io"))
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled                              := true
// ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    name                       := "privacy-computation-engine",
    libraryDependencies ++= Seq(
      dependencies.main.catsEffect,
      dependencies.main.ciris,
      dependencies.main.circe,
      dependencies.main.circeGeneric,
      dependencies.main.circeLiteral,
      dependencies.main.flyway,
      dependencies.main.doobie,
      dependencies.main.doobieHikari,
      dependencies.main.doobiePostres,
      dependencies.main.tapir,
      dependencies.main.tapirHttp4s,
      dependencies.main.tapirJsonCirce,
      dependencies.main.tapirSwagger,
      dependencies.main.http4sCirce,
      dependencies.main.http4sDsl,
      dependencies.main.http4sEmberServer,
      dependencies.main.http4sEmberClient,
      dependencies.main.logback,
      dependencies.main.janino,
      dependencies.main.log4catsSlf4j
    ),
    assembly / mainClass       := Some("io.blindnet.privacy.Main"),
    assembly / assemblyJarName := "devkit_pce.jar"
  )
