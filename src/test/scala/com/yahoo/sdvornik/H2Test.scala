package com.yahoo.sdvornik

import java.io.File
import java.net.{URL, URLClassLoader}
import java.nio.file.Paths
import java.sql.DriverManager

import org.junit.{Before, Test}
import org.scalatest.junit.JUnitSuite

/**
  * @author Serg Dvornik <sdvornik@yahoo.com>
  */
class H2Test extends JUnitSuite {

  @Before
  def beforeTest(): Unit = {

    val outputFolder =
      "C:\\Users\\sdvor_000\\Documents\\WorkspaceWorking\\WorkspaceJavaIntelliJ\\ByteCodeManipulator\\output"
    val pathToH2Driver = "h2-1.4.196-mod.jar"

    Class.forName("org.h2.Driver")
  }

  @Test
  def checkMetadata(): Unit = {

    val connection = DriverManager.getConnection("jdbc:h2:mem:test_db", "test_user", "test_pwd")

    val metaData = connection.getMetaData

    val dbProductName = metaData.getDatabaseProductName
    val dbProductVersion = metaData.getDatabaseProductVersion
    val dbMajorVersion = metaData.getDatabaseMajorVersion
    val typeInfo = metaData.getTypeInfo

    println(dbProductName)
    println(dbProductVersion)
    println(dbMajorVersion)

  }

  @Test
  def checkTransformator(): Unit = {
    println(Transformator.transform("CREATE TABLE test (id INT NOT NULL, title VARCHAR(50) NOT NULL, author VARCHAR(20) NOT NULL, s_date DATE);"))
    println(Transformator.transform("ALTER TABLE test  ADD NEW_COL CHAR(25) WITH DEFAULT '11' NOT NULL"))
    println(Transformator.transform("ALTER TABLE test  ALTER COLUMN s_date SET DATA TYPE CHAR(30)"))
    println(Transformator.transform("RENAME TABLE test to test_new"))
    println(Transformator.transform("alter table test_new activate not logged initially with empty table"))
    println(Transformator.transform("alter table test_new rename column author to employee"))
    println(Transformator.transform("select current timestamp"))

  }
}

