import sbt._
import sbt.Keys._

object BuildSettings {
  val buildVersion = "0.10.0-SNAPSHOT"

  val filter = { (ms: Seq[(File, String)]) =>
    ms filter {
      case (file, path) =>
        path != "logback.xml" && !path.startsWith("toignore") && !path.startsWith("samples")
    }
  }

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.reactivemongo",
    version := buildVersion,
    scalaVersion := "2.10.4",
    javaOptions in test ++= Seq("-Xmx512m", "-XX:MaxPermSize=512m"),
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    scalacOptions in (Compile, doc) ++= Seq("-unchecked", "-deprecation", "-diagrams", "-implicits", "-skip-packages", "samples"),
    scalacOptions in (Compile, doc) ++= Opts.doc.title("ReactiveMongo API"),
    scalacOptions in (Compile, doc) ++= Opts.doc.version(buildVersion),
    shellPrompt := ShellPrompt.buildShellPrompt,
    mappings in (Compile, packageBin) ~= filter,
    mappings in (Compile, packageSrc) ~= filter,
    mappings in (Compile, packageDoc) ~= filter) ++ Publish.settings // ++ Format.settings
}

object Publish {
  def targetRepository: Project.Initialize[Option[sbt.Resolver]] = version { (version: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (version.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }

  lazy val settings = Seq(
    publishMavenStyle := true,
    publishTo <<= targetRepository,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/sh1ng/ReactiveMongo-Queries")),
    pomExtra := (
      <scm>
        <url>git://github.com/sh1ng/ReactiveMongo-Queries</url>
        <connection>scm:git://github.com/sh1ng/ReactiveMongo-Queries</connection>
      </scm>
      <developers>
        <developer>
          <id>sh1ng</id>
          <name>Vladimir Ovsyannikov</name>
          <url>https://github.com/sh1ng</url>
        </developer>
      </developers>))
}

object Format {
  import com.typesafe.sbt.SbtScalariform._

  lazy val settings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := formattingPreferences)

  lazy val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences().
      setPreference(AlignParameters, true).
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(CompactControlReadability, false).
      setPreference(CompactStringConcatenation, false).
      setPreference(DoubleIndentClassDeclaration, true).
      setPreference(FormatXml, true).
      setPreference(IndentLocalDefs, false).
      setPreference(IndentPackageBlocks, true).
      setPreference(IndentSpaces, 2).
      setPreference(MultilineScaladocCommentsStartOnFirstLine, false).
      setPreference(PreserveSpaceBeforeArguments, false).
      setPreference(PreserveDanglingCloseParenthesis, false).
      setPreference(RewriteArrowSymbols, false).
      setPreference(SpaceBeforeColon, false).
      setPreference(SpaceInsideBrackets, false).
      setPreference(SpacesWithinPatternBinders, true)
  }
}

// Shell prompt which show the current project,
// git branch and build version
object ShellPrompt {
  object devnull extends ProcessLogger {
    def info(s: => String) {}

    def error(s: => String) {}

    def buffer[T](f: => T): T = f
  }

  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
    getOrElse "-" stripPrefix "## ")

  val buildShellPrompt = {
    (state: State) =>
      {
        val currProject = Project.extract(state).currentProject.id
        "%s:%s:%s> ".format(
          currProject, currBranch, BuildSettings.buildVersion)
      }
  }
}

object Resolvers {
  val typesafe = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/")
  val resolversList = typesafe
}

object Dependencies {
  val reactivemongo = "org.reactivemongo" %% "reactivemongo" % "0.10.0"
  val specs = "org.specs2" %% "specs2" % "2.4.2" % "test"
  val scalatest = "org.scalatest" % "scalatest_2.10" % "2.1.3" % "test"
}

object ReactiveMongoQueriesBuild extends Build {
  import BuildSettings._
  import Resolvers._
  import Dependencies._

  lazy val reactivemongoqueriesroot =
    Project(
      "ReactiveMongoQueries-Root",
      file("."),
      settings = buildSettings ++ (publishArtifact := false) ).
    aggregate(reactivemongoqueries, reactivemongoqueriesspecs)

  lazy val reactivemongoqueries = Project(
    "ReactiveMongo-Queries",
    file("queries"),
    settings = buildSettings ++ Seq(
      resolvers := resolversList,
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)
    )).
    settings(libraryDependencies += Dependencies.reactivemongo)

  lazy val reactivemongoqueriesspecs = Project(
    "ReactiveMongo-QueriesSpec",
    file("queries-spec"),
    settings = buildSettings ++ (publishArtifact := false)).
    settings(libraryDependencies += Dependencies.specs).
    settings(libraryDependencies += Dependencies.scalatest).
    dependsOn (reactivemongoqueries)
}

