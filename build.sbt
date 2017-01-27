mainClass in (Compile,run) := Some("App")

//scalaVersion       := "2.10.5"
//crossScalaVersions := Seq("2.11.7", "2.10.5")

libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-swing" % "2.10+"
    , "org.yaml" % "snakeyaml" % "1.16" 
    , "org.json4s" %% "json4s-jackson" % "3.3.0"
    , "com.github.scopt" %% "scopt" % "3.5.0"
    , "org.scalactic" %% "scalactic" % "3.0.0"
    , "org.scalatest" %% "scalatest" % "3.0.0" % "test")    


lazy val commonSettings = Seq(
  version := "0.1-SNAPSHOT",
  organization := "com.example",
  scalaVersion := "2.10.1",
  test in assembly := {}
)

lazy val app = (project in file("app")).
  settings(commonSettings: _*).
  settings(
    mainClass in assembly := Some("App")
    // more settings here ...
  )


assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}
