libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.3"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.M6" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.10.1" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "org.mongodb" %% "casbah" % "2.7.1"

libraryDependencies += "org.scalatra" %% "scalatra-json" % "2.2.2"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.6"

libraryDependencies += "org.fusesource.scalate" % "sbt-scalate-plugin_2.10" % "1.6.1"

libraryDependencies += "org.scalatra" %% "scalatra-auth" % "2.2.2"

//libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.10.0"

//libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.10.0"

seq(jelasticSettings:_*)

JelasticKeys.email in JelasticKeys.deploy := sys.env.get("JELASTIC_USERNAME").getOrElse(
sys error "Please export JELASTIC_USERNAME in your shell!"
)

JelasticKeys.password in JelasticKeys.deploy := sys.env.get("JELASTIC_PWD").getOrElse(
sys error "Please export JELASTIC_PWD in your shell!"
)

JelasticKeys.apiHoster := "app.jelasticloud.com"

JelasticKeys.environment in JelasticKeys.deploy := "enwiz"

publishTo := None
