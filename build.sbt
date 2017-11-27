name := "ByteCodeManipulator"

version := "0.1"

scalaVersion := "2.12.4"

unmanagedBase in Test := baseDirectory.value / "custom_lib"



libraryDependencies := Seq(
  "org.apache.bcel" % "bcel" % "6.1",
  "com.jason-goodwin" % "better-monads" % "0.4.0",
  "com.typesafe" % "config" % "1.3.1",
  "junit" % "junit" % "4.12" % "test",
  "org.scalatest" % "scalatest_2.12" % "3.0.4" % "test"
)