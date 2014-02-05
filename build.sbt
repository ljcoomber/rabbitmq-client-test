name := "RabbitMQHATest"

version := "1.0"

scalaVersion := "2.10.3"

resolvers ++= Seq("sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")

scalacOptions  ++= Seq("-feature")

atmosSettings

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.github.sstone" %% "amqp-client" % "1.3-ML3",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3"
)

