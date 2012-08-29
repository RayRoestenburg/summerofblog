import sbt._
import sbt.Keys._

object SummerofblogBuild extends Build {

  lazy val summerofblog = Project(
    id = "summerofblog",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "summerofblog",
      organization := "com.ray",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/releases",
      libraryDependencies ++= Seq("com.typesafe.akka" % "akka-actor"   % "2.0.2",
                                  "com.typesafe.akka" % "akka-testkit" % "2.0.2",
                                  "org.scalatest"     %% "scalatest"   % "1.8")
    )
  )
}
