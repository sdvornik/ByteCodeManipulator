package com.yahoo.sdvornik;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import static java.lang.System.out;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public class H2JavaTest {

    @Before
    public void beforeTest() throws ClassNotFoundException {

      String outputFolder =
        "C:\\Users\\sdvor_000\\Documents\\WorkspaceWorking\\WorkspaceJavaIntelliJ\\ByteCodeManipulator\\output";
      String pathToH2Driver = "h2-1.4.196-mod.jar";
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
      Class.forName("org.h2.Driver");
    }

    @Test
    public void checkH2() throws SQLException {

      Connection connection = DriverManager.getConnection("jdbc:h2:mem:test_db", "test_user", "test_pwd");

      DatabaseMetaData metaData = connection.getMetaData();

      String dbProductName = metaData.getDatabaseProductName();
      String dbProductVersion = metaData.getDatabaseProductVersion();
      int dbMajorVersion = metaData.getDatabaseMajorVersion();


      out.println(dbProductName);
      out.println(dbProductVersion);
      out.println(dbMajorVersion);
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
