package com.yahoo.sdvornik;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public class Transformator {

  private Transformator() {}

  private static Pattern[] patterns = null;
  private static String[] replacers = null;

  public static String transform(String sql) {
    int length = patterns.length;

    for(int i = 0; i < patterns.length; ++i) {
      sql = patterns[i].matcher(sql).replaceAll(replacers[i]);
    }
    return sql;
  }
}
