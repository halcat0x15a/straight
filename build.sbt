scalaVersion := "2.11.5"

fork in run := true

libraryDependencies += "org.apache.commons" % "commons-math3" % "3.4.1"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xexperimental")
