package com.tractorstore.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tractorstore.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import({
  GlobalExceptionHandler.class,
  TraceIdFilter.class,
  GlobalExceptionHandlerTest.TestController.class
})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mvc;

  @Test
  void businessNotFoundBecomes404WithItsCode() throws Exception {
    MvcResult result =
        mvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("TEST_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Thing 42 not found"))
            .andExpect(jsonPath("$.details").doesNotExist())
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void unexpectedErrorBecomes500WithoutInternals() throws Exception {
    MvcResult result =
        mvc.perform(get("/test/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value("Unexpected error"))
            .andExpect(jsonPath("$.details").doesNotExist())
            .andExpect(content().string(not(containsString("Exception"))))
            .andExpect(content().string(not(containsString("SELECT"))))
            .andExpect(content().string(not(containsString("at com."))))
            .andExpect(content().string(not(containsString("RuntimeException"))))
            .andExpect(content().string(not(containsString("TestController"))))
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void incomingTraceIdIsReusedInHeaderAndBody() throws Exception {
    mvc.perform(get("/test/not-found").header(TraceIdFilter.HEADER, "demo-trace-1"))
        .andExpect(header().string(TraceIdFilter.HEADER, "demo-trace-1"))
        .andExpect(jsonPath("$.traceId").value("demo-trace-1"));

    mvc.perform(get("/test/boom").header(TraceIdFilter.HEADER, "demo-trace-1"))
        .andExpect(header().string(TraceIdFilter.HEADER, "demo-trace-1"))
        .andExpect(jsonPath("$.traceId").value("demo-trace-1"));
  }

  private static void expectTraceIdMatchesHeader(MvcResult result) throws Exception {
    String traceId = result.getResponse().getHeader(TraceIdFilter.HEADER);
    assertThat(traceId).isNotBlank();
    jsonPath("$.traceId").value(traceId).match(result);
  }

  @RestController
  static class TestController {

    @GetMapping("/test/not-found")
    String notFound() {
      throw new TestNotFoundException("Thing 42 not found");
    }

    @GetMapping("/test/boom")
    String boom() {
      throw new RuntimeException("SELECT secret FROM x");
    }
  }

  static class TestNotFoundException extends NotFoundException {
    TestNotFoundException(String message) {
      super("TEST_NOT_FOUND", message);
    }
  }
}
