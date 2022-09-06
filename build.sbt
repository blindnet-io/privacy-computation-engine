import dependencies.*

ThisBuild / scalaVersion                                   := "3.1.3"
ThisBuild / version                                        := "0.4.0-SNAPSHOT"
ThisBuild / organization                                   := "io.blindnet"
ThisBuild / organizationName                               := "blindnet"
ThisBuild / organizationHomepage                           := Some(url("https://blindnet.io"))
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled                              := true
ThisBuild / semanticdbVersion                              := scalafixSemanticdb.revision

resolvers += Resolver.sonatypeRepo("snapshots")

val commonSettings = Seq(
  scalacOptions ++= Seq("-Xmax-inlines", "100")
)

lazy val root = (project in file("."))
  .settings(
    name := "privacy-computation-engine"
  )
  .aggregate(util, priv, core)

lazy val util = (project in file("modules/util"))
  .settings(commonSettings*)
  .settings(
    name := "pce-util",
    libraryDependencies ++= Seq(
      dependencies.main.circe,
      dependencies.main.circeGeneric
    )
  )

lazy val priv = (project in file("modules/priv"))
  .settings(commonSettings*)
  .settings(
    name := "pce-priv",
    libraryDependencies ++= Seq(
      dependencies.main.cats,
      dependencies.main.circe,
      dependencies.main.circeGeneric,
      dependencies.main.tapir,
      dependencies.main.doobie,
      dependencies.main.doobiePostgres,
      dependencies.test.scalatest,
      dependencies.test.scalaCheck
    )
  )
  .dependsOn(util)

lazy val core = (project in file("modules/core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings*)
  .settings(
    name                             := "pce-core",
    buildInfoKeys                    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage                 := "build",
    libraryDependencies ++= Seq(
      dependencies.main.catsEffect,
      dependencies.main.ciris,
      dependencies.main.circe,
      dependencies.main.circeGeneric,
      dependencies.main.circeLiteral,
      dependencies.main.flyway,
      dependencies.main.doobie,
      dependencies.main.doobieHikari,
      dependencies.main.doobiePostgres,
      dependencies.main.doobiePostgresCirce,
      dependencies.main.tapir,
      dependencies.main.tapirHttp4s,
      dependencies.main.tapirJsonCirce,
      dependencies.main.tapirSwagger,
      dependencies.main.http4sCirce,
      dependencies.main.http4sDsl,
      dependencies.main.http4sEmberServer,
      dependencies.main.http4sEmberClient,
      dependencies.main.tapir,
      dependencies.main.tapirHttp4s,
      dependencies.main.tapirJsonCirce,
      dependencies.main.tapirSwagger,
      dependencies.main.logback,
      dependencies.main.janino,
      dependencies.main.log4catsSlf4j
    ),
    assembly / mainClass             := Some("io.blindnet.pce.Main"),
    assembly / assemblyJarName       := "devkit_pce.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
        MergeStrategy.singleOrError
      case x                                                                            =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
  .dependsOn(util, priv)
