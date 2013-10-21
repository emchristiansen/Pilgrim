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
      "org.xerial" % "sqlite-jdbc" % "3.7.2",
      "org.jumpmind.symmetric.jdbc" % "mariadb-java-client" % "1.1.1",
      //      "mysql" % "mysql-connector-java" % "5.1.6",
      "com.typesafe.slick" %% "slick" % "1.0.1",
      "st.sparse" %% "sundry" % "0.1-SNAPSHOT",
      "st.sparse" %% "billy" % "0.1.1-SNAPSHOT",
      "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
      "org.slf4j" % "slf4j-simple" % "1.7.5",
      "com.chuusai" % "shapeless_2.10.2" % "2.0.0-M1",
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
      com.typesafe.sbt.SbtNativePackager.packageArchetype.java_application ++
      addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)
  //      SbtStartScript.startScriptForClassesSettings

  lazy val root = {
    val settings = libSettings ++ Seq(name := projectName, fork := true)
    Project(id = projectName, base = file("."), settings = settings)
  }
}
