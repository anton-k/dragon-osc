import sbt._

object MyBuild extends Build {

  lazy val root = Project("root", file("."))
        .dependsOn(oscProject)
        .dependsOn(uiProject)
  
  lazy val oscProject = RootProject(uri("git://github.com/anton-k/scala-simple-osc.git#master"))
  lazy val uiProject  = RootProject(uri("git://github.com/anton-k/scala-swing-audio-widgets.git#master"))

}
