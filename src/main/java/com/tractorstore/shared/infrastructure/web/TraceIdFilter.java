package com.tractorstore.shared.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request a trace id: reuses a valid incoming {@code X-Trace-Id} or generates one, puts
 * it in the MDC (log lines) and in the response header, also for errors (FR-015).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Trace-Id";
  public static final String MDC_KEY = "traceId";

  /** Restricts incoming ids so they cannot inject into logs or headers. */
  private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = traceIdOf(request);
    MDC.put(MDC_KEY, traceId);
    response.setHeader(HEADER, traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private static String traceIdOf(HttpServletRequest request) {
    String incoming = request.getHeader(HEADER);
    if (incoming != null && VALID.matcher(incoming).matches()) {
      return incoming;
    }
    return UUID.randomUUID().toString().replace("-", "");
  }
}
