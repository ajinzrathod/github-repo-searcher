package com.ajinz.githubsearch.controller;

import com.ajinz.githubsearch.dto.SearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
  void shouldSearchRepositoriesWithGetRequest() throws Exception {
    MvcResult mvcResult =
        mockMvc
            .perform(
                get("/api/github/search/repositories")
                    .param("query", "spring")
                    .param("language", "java")
                    .param("sort", "stars")
                    .param("order", "desc")
                    .param("per_page", "5"))
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
    SearchRequest invalidNoOfPagesRequest = new SearchRequest("OpenCV", null, "stars", "desc", 0, 200);

    mockMvc
        .perform(
            post("/api/github/search/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidNoOfPagesRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }
}
