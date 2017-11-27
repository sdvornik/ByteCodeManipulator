package com.yahoo.sdvornik.transformers;

import java.util.regex.Pattern;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public final class MatcherReplacer {

  public final Pattern pattern;
  public final String replacer;

  public MatcherReplacer(Pattern pattern, String replacer) {
    this.pattern = pattern;
    this.replacer = replacer;
  }
}
