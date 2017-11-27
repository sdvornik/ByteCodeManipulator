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
/*
    val outputFolder =
      "C:\\Users\\sdvor_000\\Documents\\WorkspaceWorking\\WorkspaceJavaIntelliJ\\ByteCodeManipulator\\input"
    val pathToH2Driver = "h2-1.4.196.jar"
*/
    /*
    val pathToInputJar: String = Paths.get(outputFolder).resolve(pathToH2Driver).toString
    val file: File = new File(pathToInputJar)

    val url: URL = file.toURI.toURL
    println(url)
    val urls: Array[URL] = Array(url)

    val cl: ClassLoader = new URLClassLoader(urls)



    val cls = cl.loadClass("org.h2.Driver")
    */
    Class.forName("org.h2.Driver")
  }

  @Test
  def checkH2(): Unit = {

    val connection = DriverManager.getConnection("jdbc:h2:mem:test_db", "test_user", "test_pwd")

    val metaData = connection.getMetaData

    val dbProductName = metaData.getDatabaseProductName
    val dbProductVersion = metaData.getDatabaseProductVersion
    val dbMajorVersion = metaData.getDatabaseMajorVersion
    val typeInfo = metaData.getTypeInfo

    metaData.getTables(null, null, null, Array("TABLE"))

    println(dbProductName)
    println(dbProductVersion)
    println(dbMajorVersion)
    /*
    connection.createStatement().execute("CREATE TABLE test (id INT NOT NULL, title VARCHAR(50) NOT NULL, author VARCHAR(20) NOT NULL, s_date DATE);");

    connection.createStatement().execute("ALTER TABLE test  ADD NEW_COL CHAR(25) WITH DEFAULT '11' NOT NULL")
    connection.createStatement().execute("ALTER TABLE test  ALTER COLUMN s_date SET DATA TYPE CHAR(30)")
    connection.createStatement().execute("RENAME TABLE test to test_new")
    connection.createStatement().execute("alter table test_new activate not logged initially with empty table")
    connection.createStatement().execute("alter table test_new rename column author to employee")
    connection.createStatement().execute("select current timestamp")
*/
  }
}

