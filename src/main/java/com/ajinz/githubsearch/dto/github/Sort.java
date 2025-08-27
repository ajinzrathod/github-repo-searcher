package com.ajinz.githubsearch.dto.github;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Sort {
  STARS("stars"),
  FORKS("forks"),
  UPDATED("updated");

  private final String value;

  Sort(String value) {
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
