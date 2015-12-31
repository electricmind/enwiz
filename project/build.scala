import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

object EnwizBuild extends Build {
  val Organization = "ru.wordmetrix"
  val Name = "EnWiz"
  val Version = "0.1.1-SNAPSHOT"
  val ScalaVersion = "2.11.7"
  val ScalatraVersion = "2.4.0"

  lazy val project = Project (
    "enwiz",
    file("."),
    settings = Defaults.defaultSettings ++ ScalatraPlugin.scalatraWithJRebel ++ scalateSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "container",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
      ),
      scalateTemplateConfig in Compile <<= (sourceDirectory in Compile){ base =>
        Seq(
          TemplateConfig(
            base / "webapp" / "WEB-INF" / "templates",
            Seq.empty,  /* default imports should be added here */
            Seq(
              Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
            ),  /* add extra bindings here */
            Some("templates")
          )
        )
      }
    )
  )
}
/*

lazy val scalate_plugin = "org.fusesource.scalate" % "sbt-scalate-plugin_2.10" % "1.6.1"
And then in your WebProject, you will need to add the org.fusesource.scalate.sbt.PrecompilerWebProject trait. And then make sure the Scalate dependencies are added to the project. For example:
class Project(info: ProjectInfo) extends 
      DefaultWebProject(info) with 
      PrecompilerWebProject {

  lazy val scalate_core = "org.fusesource.scalate" % "scalate-core_2.10" % "1.6.1" 
  lazy val servlet = "javax.servlet" % "servlet-api"% "2.5" 
  lazy val logback = "ch.qos.logback" % "logback-classic" % "0.9.26"
*/