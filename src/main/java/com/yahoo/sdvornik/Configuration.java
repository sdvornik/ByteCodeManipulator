package com.yahoo.sdvornik;

import com.jasongoodwin.monads.Try;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.yahoo.sdvornik.transformers.MatcherReplacer;

import java.io.File;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.System.out;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public class Configuration {

  private final MatcherReplacer[] sqlMatchersReplacers;

  public final String[] matchers;
  public final String[] replacers;

  public final String databaseProductName;
  public final String databaseProductVersion;
  public final Integer databaseMajorVersion;

  public Configuration(String pathToConf) {

    Config conf = ConfigFactory.parseFile(new File(pathToConf));

    // read metadata transformation from config
    databaseProductName = Try.ofFailable(() -> conf.getString("DatabaseProductName")).toOptional().orElse(null);

    databaseProductVersion = Try.ofFailable(() -> conf.getString("DatabaseProductVersion")).toOptional().orElse(null);

    databaseMajorVersion = Try.ofFailable(() -> conf.getInt("DatabaseMajorVersion")).toOptional().orElse(null);

    // read sql transformation from config
    sqlMatchersReplacers = Try.ofFailable(() ->
      conf
        .getConfigList("sql")
        .stream()
        .map(elm ->
          new MatcherReplacer(
            elm.getString("Match"),
            elm.getString("Replace")
          )
        ).toArray(MatcherReplacer[]::new)
    ).toOptional().orElse(null);

    if(sqlMatchersReplacers != null) {
      matchers = Stream.of(sqlMatchersReplacers).map(mr -> mr.pattern).toArray(size -> new String[size]);
      replacers = Stream.of(sqlMatchersReplacers).map(mr -> mr.replacer).toArray(size -> new String[size]);
    }
    else {
      matchers = null;
      replacers = null;
    }

    out.println("Successfully init driver configuration.");
  }

}
