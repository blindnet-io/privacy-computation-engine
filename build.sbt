import dependencies.*

ThisBuild / scalaVersion         := "3.1.3"
ThisBuild / version              := "0.1.0-SNAPSHOT"
ThisBuild / organization         := "io.blindnet"
ThisBuild / organizationName     := "blindnet"
ThisBuild / organizationHomepage := Some(url("https://blindnet.io"))

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    name                       := "privacy-computation-engine",
    scalacOptions              := List("-Ymacro-annotations"),
    libraryDependencies ++= Seq(
      dependencies.main.catsEffect,
      dependencies.main.circe,
      dependencies.main.circeGeneric,
      dependencies.main.circeLiteral,
      dependencies.main.flyway,
      dependencies.main.doobie,
      dependencies.main.doobieHikari,
      dependencies.main.doobiePostres
    ),
    assembly / mainClass       := Some("io.blindnet.privacy.Main"),
    assembly / assemblyJarName := "privacy_computation_engine.jar"
  )
