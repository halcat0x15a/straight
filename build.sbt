scalaVersion := "2.11.6"

version := "0.2"

fork in run := true

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-math3" % "3.4.1",
  "org.json4s" %% "json4s-native" % "3.2.10"
)

scalacOptions ++= Seq("-deprecation", "-feature", "-Xexperimental")
