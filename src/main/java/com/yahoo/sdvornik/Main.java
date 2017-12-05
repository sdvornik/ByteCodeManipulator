package com.yahoo.sdvornik;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public class Main {

  private static Path inputFolder = Paths.get(".").resolve("input");
  private static Path outputFolder = Paths.get(".").resolve("output");

  private static String driverName = "mysql-connector-java-5.1.45.jar";
  // "mysql-connector-java-8.0.8-dmr.jar";
  // mysql-connector-java-5.1.45.jar
  // "h2-1.4.196.jar";
  private static String configFilename = "driver.conf";

  public static void main(String[] args) throws Exception {
    System.out.println(Paths.get(".").toAbsolutePath().toString());

    String pathToInputJar = inputFolder.resolve(driverName).toString();
    String pathToConfig = inputFolder.resolve(configFilename).toString();
    Configuration conf = new Configuration(pathToConfig);

    JarTransformer reader = new JarTransformer(pathToInputJar, outputFolder, conf);
    reader.extract();
  }

}
