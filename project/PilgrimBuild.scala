import sbt._
//import com.typesafe.sbt.packager.Keys._
import sbt.Keys._
//import com.typesafe.sbt.SbtNativePackager._

//import com.typesafe.sbt.SbtStartScript

object PilgrimBuild extends Build {
  def extraResolvers = Seq(
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"),
      //      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      //      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      //     "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/",
      //      "repo.codahale.com" at "http://repo.codahale.com",
      "Akka Repository" at "http://repo.akka.io/releases/",
      //      "spray-io" at "http://repo.spray.io/",
      "typesafe-releases" at "http://repo.typesafe.com/typesafe/repo",
      "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + "/.m2/repository"))

  val projectName = "Pilgrim"

  val scalaVersionString = "2.10.3"

  def extraLibraryDependencies = Seq(
    libraryDependencies ++= Seq(
      "st.sparse" %% "sundry" % "0.1-SNAPSHOT",
      "org.rogach" %% "scallop" % "0.9.4"))

  def updateOnDependencyChange = Seq(
    watchSources <++= (managedClasspath in Test) map { cp => cp.files })

  def scalaSettings = Seq(
    scalaVersion := scalaVersionString,
    scalacOptions ++= Seq(
      "-optimize",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:implicitConversions",
      "-language:higherKinds",
      // "-language:reflectiveCalls",
      "-language:postfixOps",
      "-language:existentials",
      "-Xlint",
      //      "-Xlog-implicits",
      "-Yinline-warnings"))

  def libSettings =
    Project.defaultSettings ++
      extraResolvers ++
      extraLibraryDependencies ++
      scalaSettings ++
      updateOnDependencyChange ++
      com.typesafe.sbt.SbtNativePackager.packageArchetype.java_application
//      SbtStartScript.startScriptForClassesSettings

  lazy val root = {
    val settings = libSettings ++ Seq(name := projectName, fork := true)
    Project(id = projectName, base = file("."), settings = settings)
  }
}
