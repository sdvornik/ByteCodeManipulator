package com.yahoo.sdvornik

import java.sql.DriverManager

import org.junit.{Before, Test}
import org.scalatest.junit.JUnitSuite

/**
  * @author Serg Dvornik <sdvornik@yahoo.com>
  */
class H2Test extends JUnitSuite {

  @Before
  def beforeTest(): Unit = {
    Class.forName("org.h2.Driver")
  }

  @Test
  def checkMetadata(): Unit = {

    val connection = DriverManager.getConnection("jdbc:h2:mem:test_db", "test_user", "test_pwd")

    val metaData = connection.getMetaData

    val dbProductName = metaData.getDatabaseProductName
    val dbProductVersion = metaData.getDatabaseProductVersion
    val dbMajorVersion = metaData.getDatabaseMajorVersion

    println(dbProductName)
    println(dbProductVersion)
    println(dbMajorVersion)

    connection.createStatement().execute("CREATE TABLE test (id INT NOT NULL, title VARCHAR(50) NOT NULL, author VARCHAR(20) NOT NULL, s_date DATE);")
    connection.createStatement().execute("alter table test add new_col CHAR(25) with default '11' NOT NULL")
    connection.createStatement().execute("alter table test  ALTER COLUMN s_date SET DATA TYPE CHAR(30)")
    connection.createStatement().execute("RENAME TABLE test to test_new")
    connection.createStatement().execute("alter table test_new activate not logged initially with empty table")
    connection.createStatement().execute("alter table test_new rename column author to employee")
    connection.createStatement().execute("select current timestamp")



  }

}

