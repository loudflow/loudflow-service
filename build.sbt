organization in ThisBuild := "com.loudflow"
version in ThisBuild := "0.1.0"

scalaVersion in ThisBuild := "2.12.10"

scalacOptions += "-Ypartial-unification -deprecation -unchecked"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val cats = "org.typelevel" %% "cats-core" % "2.0.0-RC1"
val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"
val scalaGraph = "org.scala-graph" %% "graph-core" % "1.13.0"
val scalaGraphJson = "org.scala-graph" %% "graph-json" % "1.12.1"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8" % "test"
val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
val sangria = "org.sangria-graphql" %% "sangria" % "1.4.2"

lazy val `loudflow-service` = (project in file("."))
  .aggregate(`common`, `agent-api`, `agent-impl`, `model-api`, `model-impl`, `simulation-api`, `simulation-impl`)

lazy val `common` = (project in file("common"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPersistenceCassandra,
      scalaGraph,
      scalaGraphJson,
      cats,
      catsEffect,
      scalaTest,
      scalaLogging,
      logback
    )
  )

lazy val `agent-api` = (project in file("agent-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      logback
    )
  )
  .dependsOn(`common`)

lazy val `agent-impl` = (project in file("agent-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      cats,
      scalaTest,
      scalaLogging,
      logback
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`common`, `agent-api`, `model-api`)

lazy val `model-api` = (project in file("model-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      scalaLogging,
      logback,
      sangria
    )
  )
  .dependsOn(`common`)

lazy val `model-impl` = (project in file("model-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      cats,
      scalaTest,
      scalaLogging,
      logback
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`common`, `model-api`, `simulation-api`)

lazy val `simulation-api` = (project in file("simulation-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      scalaLogging,
      logback
    )
  )
  .dependsOn(`common`)

lazy val `simulation-impl` = (project in file("simulation-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      cats,
      scalaTest,
      scalaLogging,
      logback
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`common`, `simulation-api`, `model-api`)
