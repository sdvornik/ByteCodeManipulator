package com.yahoo.sdvornik.transformers;

import java.util.regex.Pattern;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public final class MatcherReplacer {

  public final String pattern;
  public final String replacer;

  public MatcherReplacer(String pattern, String replacer) {
    this.pattern = pattern;
    this.replacer = replacer;
  }
}
