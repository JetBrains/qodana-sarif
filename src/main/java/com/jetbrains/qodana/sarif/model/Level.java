package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.SerializedName;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

/**
 * Specifies the failure level for the report.
 */
@Generated("jsonschema2pojo")
public enum Level {

  @SerializedName("none")
  NONE("none"),
  @SerializedName("note")
  NOTE("note"),
  @SerializedName("warning")
  WARNING("warning"),
  @SerializedName("error")
  ERROR("error");
  private final String value;
  private final static Map<String, Level> CONSTANTS = new HashMap<String, Level>();

  static {
    for (Level c : values()) {
      CONSTANTS.put(c.value, c);
    }
  }

  private Level(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  public String value() {
    return this.value;
  }

  public static Level fromValue(String value) {
    Level constant = CONSTANTS.get(value);
    if (constant == null) {
      throw new IllegalArgumentException(value);
    }
    else {
      return constant;
    }
  }
}