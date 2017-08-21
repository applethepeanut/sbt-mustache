import bintray.Keys._

val releaseVersion = "0.4-SNAPSHOT"
val scalatestVersion = "3.0.4"
val logbackClassicVersion = "1.2.3"

lazy val `sbt-mustache` = project
  .in(file("."))
  .aggregate(generator, api, plugin)
  .settings(commonSettings: _*)
  .settings(crossScala: _*)
  .settings(noPublish: _*)

//Compiles and Renders Mustache html templates
lazy val api = project
  .in(file("api"))
  .settings(commonSettings: _*)
  .settings(crossScala: _*)
  .settings(publishMaven: _*)
  .settings(
    name := "sbt-mustache-api",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalatestVersion % "test",
      "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
      "com.github.spullara.mustache.java" % "scala-extensions-2.11" % "0.9.5",
      "com.github.spullara.mustache.java" % "compiler" % "0.9.5"
    ),
    initLoggerInTests()
  )

//Handles generation of Scala files from Mustache html templates
lazy val generator = project
  .in(file("generator"))
  .dependsOn(api)
  .settings(commonSettings: _*)
  .settings(crossScala: _*)
  .settings(publishMaven: _*)
  .settings(
    name := "sbt-mustache-generator",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalatestVersion % "test",
      "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-sbt" % "io" % "0.13.8"
    ),
    fork in run := true,
    initLoggerInTests()
  )

lazy val plugin = project
  .in(file("plugin"))
  .settings(commonSettings: _*)
  .settings(crossScala: _*)
  .settings(publishSbtPlugin: _*)
  .settings(scriptedSettings: _*)
  .settings(
    name := "sbt-mustache",
    scalaVersion := "2.10.6",
    scriptedLaunchOpts += ("-Dproject.version=" + version.value),
    scriptedLaunchOpts += "-XX:MaxPermSize=256m",
    scriptedBufferLog := false,
    sbtPlugin := true,
    resourceGenerators in Compile <+= generateVersionFile,
    libraryDependencies ++= Seq(
      "io.michaelallen.mustache" % "sbt-mustache-generator_2.11" % releaseVersion
    )
  )

def commonSettings = Seq(
  organization := "io.michaelallen.mustache",
  version := releaseVersion,
  scalaVersion := sys.props.get("scala.version").getOrElse("2.11.8"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
  resolvers += Resolver.mavenLocal
)

def noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishTo := Some(Resolver.file("no-publish", crossTarget.value / "no-publish"))
)

def publishSbtPlugin = bintrayPublishSettings ++ Seq(
  publishMavenStyle := false,
  repository in bintray := "sbt-plugins",
  bintrayOrganization in bintray := None,
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
)

def publishMaven = bintrayPublishSettings ++ Seq(
  publishMavenStyle := true,
  repository in bintray := "maven",
  bintrayOrganization in bintray := None,
  homepage := Some(url("https://github.com/michaeldfallen/sbt-mustache")),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  pomExtra := {
    <scm>
      <url>https://github.com/michaeldfallen/sbt-mustache</url>
      <connection>scm:git:git@github.com:michaeldfallen/sbt-mustache.git</connection>
    </scm>
      <developers>
        <developer>
          <id>michaeldfallen</id>
          <name>Michael Allen</name>
          <url>https://github.com/michaeldfallen</url>
        </developer>
      </developers>
  },
  pomIncludeRepository := { _ => false }
)

def generateVersionFile = Def.task {
  val version = (Keys.version in api).value
  val file = (resourceManaged in Compile).value / "mustache.version.properties"
  val content = s"mustache.api.version=$version"
  IO.write(file, content)
  Seq(file)
}

def crossScala = Seq(
  crossScalaVersions := Seq(scalaVersion.value),
  unmanagedSourceDirectories in Compile += {
    (sourceDirectory in Compile).value / ("scala-" + scalaBinaryVersion.value)
  }
)

def initLoggerInTests() = testOptions in Test += Tests.Setup(classLoader =>
  classLoader
    .loadClass("org.slf4j.LoggerFactory")
    .getMethod("getLogger", classLoader.loadClass("java.lang.String"))
    .invoke(null, "ROOT")
)
