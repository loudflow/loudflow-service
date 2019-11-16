organization in ThisBuild := "com.loudflow"
version in ThisBuild := "0.1.0"

scalaVersion in ThisBuild := "2.12.10"

scalacOptions += "-Ypartial-unification -deprecation -unchecked"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val cats = "org.typelevel" %% "cats-core" % "2.0.0-RC1"
val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"
val accord = "com.wix" %% "accord-core" % "0.7.3"
val scalaGraph = "org.scala-graph" %% "graph-core" % "1.13.0"
val scalaGraphJson = "org.scala-graph" %% "graph-json" % "1.12.1"
val neo4j = "org.neo4j" % "neo4j" % "3.5.11"
val neo4jBolt = "org.neo4j" % "neo4j-bolt" % "3.5.11"
val neotypes = "com.dimafeng" %% "neotypes" % "0.4.0"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lazy val `loudflow` = (project in file("."))
  .aggregate(`common`, `simulation-api`, `simulation-impl`, `model-api`, `model-impl`)

lazy val `common` = (project in file("common"))
//  .settings(
//    Compile / unmanagedJars ++= (baseDirectory.value / "lib" ** "*.jar").classpath
//  )
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPersistenceCassandra,
      scalaGraph,
      scalaGraphJson,
      neo4j,
      neo4jBolt,
      neotypes,
      accord,
      cats,
      catsEffect,
      scalaTest
    )
  )

lazy val `model-api` = (project in file("model-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      accord
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
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`common`, `model-api`, `simulation-api`)

lazy val `simulation-api` = (project in file("simulation-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      accord
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
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`common`, `simulation-api`, `model-api`)

