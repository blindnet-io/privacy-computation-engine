import scala.concurrent.duration.*
import lmcoursier.definitions.CachePolicy
import dependencies.*

ThisBuild / scalaVersion                                   := "3.1.3"
ThisBuild / version                                        := "0.5.0-SNAPSHOT"
ThisBuild / organization                                   := "io.blindnet"
ThisBuild / organizationName                               := "blindnet"
ThisBuild / organizationHomepage                           := Some(url("https://blindnet.io"))
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled                              := true
ThisBuild / semanticdbVersion                              := scalafixSemanticdb.revision

Test / fork := true

// hooks are added after sbt has started for the first time
Global / onLoad ~= (_ andThen ("writeHooks" :: _))
lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)

val commonSettings = Seq(
  scalacOptions ++= Seq("-Xmax-inlines", "100"),
  scalacOptions ++= Seq("-deprecation")
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
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    csrConfiguration := csrConfiguration.value.withTtl(Some(0.seconds)),
    libraryDependencies ++= Seq(
      dependencies.main.circe,
      dependencies.main.circeGeneric
    )
  )

lazy val priv = (project in file("modules/priv"))
  .settings(commonSettings*)
  .settings(
    name := "pce-priv",
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    csrConfiguration := csrConfiguration.value.withTtl(Some(0.seconds)),
    libraryDependencies ++= Seq(
      dependencies.main.cats,
      dependencies.main.circe,
      dependencies.main.circeGeneric,
      dependencies.main.tapir,
      dependencies.main.doobie,
      dependencies.main.doobiePostgres,
      dependencies.test.scalaCheck,
      dependencies.test.weaver
    )
  )
  .dependsOn(util)

lazy val core = (project in file("modules/core"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(commonSettings*)
  .settings(
    name                             := "pce-core",
    buildInfoKeys                    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage                 := "build",
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    resolvers += "Blindnet Snapshots" at "https://nexus.blindnet.io/repository/maven-snapshots",
    // https://github.com/sbt/sbt/issues/5377
    csrConfiguration := csrConfiguration.value.withTtl(Some(0.seconds)),
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
      dependencies.main.redis4cats,
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
      dependencies.main.log4catsSlf4j,
      dependencies.main.identityClient,
      dependencies.test.scalaCheck,
      dependencies.test.weaver,
      dependencies.test.testContainers,
      dependencies.test.testContainersPosgres
    ),
    assembly / mainClass             := Some("io.blindnet.pce.Main"),
    assembly / assemblyJarName       := "devkit_pce.jar",
    assembly / assemblyMergeStrategy := {
      case PathList(ps @ _*) if ps.last == "module-info.class"                          =>
        MergeStrategy.discard
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
        MergeStrategy.singleOrError
      case x                                                                            =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
  .settings(
    Docker / packageName := "pce",
    dockerBaseImage      := "eclipse-temurin:17",
    makeBatScripts       := Nil,
    dockerUpdateLatest   := true
  )
  .dependsOn(util, priv)
