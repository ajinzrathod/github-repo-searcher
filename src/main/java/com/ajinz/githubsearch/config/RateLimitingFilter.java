package com.ajinz.githubsearch.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!test & !integrationTest")
@Component
public class RateLimitingFilter implements Filter {

  private static final int MAX_REQUESTS = 10;
  private final ConcurrentHashMap<String, Long> requests = new ConcurrentHashMap<>();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (req.getRequestURI().contains("/health")) {
      chain.doFilter(request, response);
      return;
    }

    String ip = req.getRemoteAddr();
    long now = System.currentTimeMillis();
    long minute = now / 60000;
    String key = ip + ":" + minute;

    if (requests.merge(key, 1L, Long::sum) > MAX_REQUESTS) {
      res.setStatus(429);
      res.getWriter().write("{\"error\":\"Too many requests\"}");
      return;
    }

    chain.doFilter(request, response);
  }
}
