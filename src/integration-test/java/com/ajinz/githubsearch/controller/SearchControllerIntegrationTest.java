package com.ajinz.githubsearch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ajinz.githubsearch.dto.github.GithubSearchRequest;
import com.ajinz.githubsearch.dto.github.Order;
import com.ajinz.githubsearch.dto.github.Sort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldReturnHealthStatus() throws Exception {
    mockMvc
        .perform(get("/api/github/health"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  @Test
  void shouldSearchRepositoriesWithPostRequest() throws Exception {
    GithubSearchRequest searchRequest =
        new GithubSearchRequest("spring", "java", Sort.STARS, Order.DESC, 1, 5);

    MvcResult mvcResult =
        mockMvc
            .perform(
                post("/api/github/search/repositories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc
        .perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.items").isArray());
  }

  @Test
  void shouldReturnValidationErrorForInvalidRequest() throws Exception {
    GithubSearchRequest invalidRequest =
        new GithubSearchRequest("OpenCV", null, Sort.FORKS, Order.DESC, 35, 200);

    mockMvc
        .perform(
            post("/api/github/search/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }
}
