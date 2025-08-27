package com.ajinz.githubsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GithubsearchApplication {

  public static void main(String[] args) {
    SpringApplication.run(GithubsearchApplication.class, args);
    System.out.println("GitHub Search API is running!");
  }
}
