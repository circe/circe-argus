import ReleaseTransformations._

ThisBuild / crossScalaVersions := Seq("2.12.14", "2.13.15")
ThisBuild / scalaVersion := crossScalaVersions.value.last
ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List("clean", "coverage", "test", "coverageReport"),
    id = None,
    name = Some("test")
  ),
  WorkflowStep.Use(
    UseRef.Public(
      "codecov",
      "codecov-action",
      "v1"
    )
  )
)

lazy val Vers = new {
  val circe = "0.14.1"
  val scalatest = "3.2.11"
}

lazy val commonSettings = Seq(
  name := "Argus",
  organization := "io.circe",
  scalacOptions ++= Seq("-target:jvm-1.8") ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => Seq("-Ymacro-annotations")
      case _                       => Seq("-Ypartial-unification")
    }
  },
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n < 13 =>
        Seq(compilerPlugin(("org.scalamacros" %% "paradise" % "2.1.1").cross(CrossVersion.full)))
      case _ => Seq.empty
    }
  },
  homepage := Some(url("https://github.com/aishfenton/Argus")),
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/MIT")),
  // NB: We put example schemas in main package since otherwise the macros can't run for test (since they
  // execute before test-classes is populated). But then we need to exclude them from packing.
  Compile / packageBin / mappings ~= {
    _.filter(!_._1.getName.endsWith(".json"))
  },
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  Test / publishArtifact := false,
  pomIncludeRepository := (_ => false),
  sonatypeProfileName := "com.github.aishfenton",
  pomExtra :=
    <scm>
      <url>git@github.com:aishfenton/Argus.git</url>
      <connection>scm:git:git@github.com:aishfenton/Argus.git</connection>
    </scm>
      <developers>
        <developer>
          <id>aishfenton</id>
          <name>Aish Fenton</name>
        </developer>
        <developer>
          <id>datamusing</id>
          <name>Sudeep Das</name>
        </developer>
        <developer>
          <id>dbtsai</id>
          <name>DB Tsai</name>
        </developer>
        <developer>
          <id>rogermenezes</id>
          <name>Roger Menezes</name>
        </developer>
      </developers>,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
    pushChanges
  )
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val argus = project
  .settings(moduleName := "circe-argus")
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
      "io.circe" %% "circe-core" % Vers.circe,
      "io.circe" %% "circe-generic" % Vers.circe,
      "io.circe" %% "circe-parser" % Vers.circe,
      "org.scalactic" %% "scalactic" % Vers.scalatest % Test,
      "org.scalatest" %% "scalatest" % Vers.scalatest % Test
    )
  )
  .dependsOn(runtime % Test)

lazy val runtime = project
  .settings(moduleName := "circe-argus-runtime")
  .settings(commonSettings: _*)

lazy val root = project
  .in(file("."))
  .aggregate(argus, runtime)
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)

// Clears screen between refreshes in continuous mode
maxErrors := 5
watchTriggeredMessage := Watch.clearScreenOnTrigger
