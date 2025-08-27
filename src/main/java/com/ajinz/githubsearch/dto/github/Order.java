package com.ajinz.githubsearch.dto.github;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Order {
  ASC("asc"),
  DESC("desc");

  private final String value;

  Order(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
