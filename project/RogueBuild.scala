// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.
import sbt._
import Keys._

object RogueBuild extends Build {
  override lazy val projects =
    Seq(all, field, core, lift)

  lazy val all: Project = Project("all", file(".")) aggregate(
    field, core, lift)

  lazy val field = Project("rogue-field", file("rogue-field/")) dependsOn()
  lazy val core = Project("rogue-core", file("rogue-core/")) dependsOn(field % "compile;test->test;runtime->runtime")
  lazy val lift = Project("rogue-lift", file("rogue-lift/")) dependsOn(core % "compile;test->test;runtime->runtime")

  lazy val defaultSettings: Seq[Setting[_]] = Seq(
    version := "2.0.0-beta10",
    organization := "com.foursquare",
    crossScalaVersions := Seq("2.9.1", "2.9.0-1", "2.9.0"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    publishTo <<= (version) { v =>
      val nexus = "http://oss.sonatype.org/"
      if (v.endsWith("-SNAPSHOT"))
        Some("snapshots" at nexus+"content/repositories/snapshots")
      else
        Some("releases" at nexus+"service/local/staging/deploy/maven2")
    },
    pomExtra := (
      <url>http://github.com/foursquare/rogue</url>
      <licenses>
        <license>
          <name>Apache</name>
          <url>http://www.opensource.org/licenses/Apache-2.0</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:foursquare/rogue.git</url>
        <connection>scm:git:git@github.com:foursquare/rogue.git</connection>
      </scm>
      <developers>
        <developer>
          <id>jliszka</id>
          <name>Jason Liszka</name>
          <url>http://github.com/jliszka</url>
        </developer>
      </developers>),
    resolvers += "Bryan J Swift Repository" at "http://repos.bryanjswift.com/maven2/",
    resolvers <++= (version) { v =>
      if (v.endsWith("-SNAPSHOT"))
        Seq(ScalaToolsSnapshots)
      else
        Seq()
    },
    retrieveManaged := true,
    scalacOptions ++= Seq("-deprecation", "-unchecked"),

    // Hack to work around SBT bug generating scaladoc for projects with no dependencies.
    // https://github.com/harrah/xsbt/issues/85
    unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist")),

    testFrameworks += new TestFramework("com.novocode.junit.JUnitFrameworkNoMarker"),
    credentials ++= {
      val sonatype = ("Sonatype Nexus Repository Manager", "oss.sonatype.org")
      def loadMavenCredentials(file: java.io.File) : Seq[Credentials] = {
        xml.XML.loadFile(file) \ "servers" \ "server" map (s => {
          val host = (s \ "id").text
          val realm = if (host == sonatype._2) sonatype._1 else "Unknown"
          Credentials(realm, host, (s \ "username").text, (s \ "password").text)
        })
      }
      val ivyCredentials   = Path.userHome / ".ivy2" / ".credentials"
      val mavenCredentials = Path.userHome / ".m2"   / "settings.xml"
      (ivyCredentials.asFile, mavenCredentials.asFile) match {
        case (ivy, _) if ivy.canRead => Credentials(ivy) :: Nil
        case (_, mvn) if mvn.canRead => loadMavenCredentials(mvn)
        case _ => Nil
      }
    })
}
