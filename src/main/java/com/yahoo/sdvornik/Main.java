package com.yahoo.sdvornik;

import java.nio.file.Paths;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public class Main {

  private static String sourceFolder = "C:\\Users\\sdvor_000\\Documents\\WorkspaceWorking\\WorkspaceJavaIntelliJ\\ByteCodeManipulator\\input";
  private static String outputFolder =
    "C:\\Users\\sdvor_000\\Documents\\WorkspaceWorking\\WorkspaceJavaIntelliJ\\ByteCodeManipulator\\output";
  private static String h2DriverName = "h2-1.4.196.jar";
  private static String mysqlFilename = "mysql-connector-java-8.0.8-dmr.jar";
  private static String configFilename = "driver.conf";

  public static void main(String[] args) throws Exception {

    String pathToInputJar = Paths.get(sourceFolder).resolve(h2DriverName).toString();
    String pathToConfig = Paths.get(sourceFolder).resolve(configFilename).toString();
    Configuration conf = new Configuration(pathToConfig);

    JarTransformer reader = new JarTransformer(pathToInputJar, outputFolder, conf);
    reader.extract();
  }

}
