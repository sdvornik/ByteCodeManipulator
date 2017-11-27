package com.yahoo.sdvornik;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public class Transformator {

  private static Pattern[] patterns = new Pattern[]{Pattern.compile("a"), Pattern.compile("b")};
  private static String[] replacers = new String[]{"c", "d"};

  public static String transform(String sql) {
    int length = patterns.length;

    for(int i = 0; i < patterns.length; ++i) {
      sql = patterns[i].matcher(sql).replaceAll(replacers[i]);
    }
    return sql;
  }
}
