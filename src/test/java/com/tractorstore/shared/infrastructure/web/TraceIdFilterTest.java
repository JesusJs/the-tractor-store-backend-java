package com.tractorstore.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

  private final TraceIdFilter filter = new TraceIdFilter();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final AtomicReference<String> headerSeenByChain = new AtomicReference<>();
  private final AtomicReference<String> mdcSeenByChain = new AtomicReference<>();

  private final FilterChain recordingChain =
      (req, res) -> {
        headerSeenByChain.set(response.getHeader(TraceIdFilter.HEADER));
        mdcSeenByChain.set(MDC.get(TraceIdFilter.MDC_KEY));
      };

  @Test
  void generatesTraceIdWhenHeaderIsMissing() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), response, recordingChain);

    assertThat(response.getHeader(TraceIdFilter.HEADER)).matches("[0-9a-f]{32}");
    assertThat(headerSeenByChain.get()).isEqualTo(response.getHeader(TraceIdFilter.HEADER));
    assertThat(mdcSeenByChain.get()).isEqualTo(response.getHeader(TraceIdFilter.HEADER));
  }

  @Test
  void reusesValidIncomingTraceId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.HEADER, "demo-trace-1");

    filter.doFilter(request, response, recordingChain);

    assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("demo-trace-1");
    assertThat(headerSeenByChain.get()).isEqualTo("demo-trace-1");
    assertThat(mdcSeenByChain.get()).isEqualTo("demo-trace-1");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "a b",
        "x\ny"
      })
  void ignoresInvalidIncomingTraceId(String invalid) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.HEADER, invalid);

    filter.doFilter(request, response, recordingChain);

    assertThat(response.getHeader(TraceIdFilter.HEADER)).matches("[0-9a-f]{32}");
  }

  @Test
  void acceptsSixtyFourCharacters() throws Exception {
    String max = "a".repeat(64);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.HEADER, max);

    filter.doFilter(request, response, recordingChain);

    assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo(max);
  }

  @Test
  void clearsMdcAfterTheChain() throws Exception {
    filter.doFilter(new MockHttpServletRequest(), response, recordingChain);

    assertThat(mdcSeenByChain.get()).isNotNull();
    assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void clearsMdcEvenWhenTheChainThrows() {
    FilterChain failing =
        (req, res) -> {
          throw new IllegalStateException("boom");
        };

    assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), response, failing))
        .isInstanceOf(IllegalStateException.class);
    assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
  }
}
