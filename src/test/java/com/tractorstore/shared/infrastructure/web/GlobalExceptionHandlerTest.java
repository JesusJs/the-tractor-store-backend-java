package com.tractorstore.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tractorstore.shared.error.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import({
  GlobalExceptionHandler.class,
  TraceIdFilter.class,
  GlobalExceptionHandlerTest.TestController.class
})
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

  private static final String TOO_LONG = "X".repeat(21);

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

  @Test
  void invalidBodyIsValidationErrorWithFieldDetails() throws Exception {
    MvcResult result =
        expectError(
                mvc.perform(
                    post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")),
                400,
                "VALIDATION_ERROR")
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.details.name").value("must not be blank"))
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void tooLongPathVariableIsValidationErrorWithParameterDetails() throws Exception {
    MvcResult result =
        expectError(mvc.perform(get("/test/path/" + TOO_LONG)), 400, "VALIDATION_ERROR")
            .andExpect(jsonPath("$.details.id").value("size must be between 0 and 20"))
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void tooLongListElementIsValidationErrorWithParameterDetails() throws Exception {
    MvcResult result =
        expectError(
                mvc.perform(get("/test/list").param("skus", "AU-01-SI," + TOO_LONG)),
                400,
                "VALIDATION_ERROR")
            .andExpect(jsonPath("$.details.skus").value("size must be between 0 and 20"))
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void unreadableJsonIsMalformedRequest() throws Exception {
    MvcResult result =
        expectError(
                mvc.perform(
                    post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{nope")),
                400,
                "MALFORMED_REQUEST")
            .andExpect(jsonPath("$.details").doesNotExist())
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void wrongParameterTypeIsMalformedRequest() throws Exception {
    MvcResult result =
        expectError(mvc.perform(get("/test/number").param("n", "abc")), 400, "MALFORMED_REQUEST")
            .andExpect(jsonPath("$.details").doesNotExist())
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void missingRequiredParameterIsMalformedRequest() throws Exception {
    MvcResult result =
        expectError(mvc.perform(get("/test/number")), 400, "MALFORMED_REQUEST")
            .andExpect(jsonPath("$.details").doesNotExist())
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void unknownRouteIsNotFound() throws Exception {
    MvcResult result =
        expectError(mvc.perform(get("/test/nowhere")), 404, "NOT_FOUND")
            .andExpect(jsonPath("$.details").doesNotExist())
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void unsupportedMethodIsMethodNotAllowed() throws Exception {
    MvcResult result =
        expectError(mvc.perform(post("/test/not-found")), 405, "METHOD_NOT_ALLOWED")
            .andExpect(jsonPath("$.details").doesNotExist())
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void dataAccessErrorBecomes500WithoutInternals() throws Exception {
    MvcResult result =
        expectError(mvc.perform(get("/test/db")), 500, "INTERNAL_ERROR")
            .andExpect(jsonPath("$.message").value("Unexpected error"))
            .andExpect(jsonPath("$.details").doesNotExist())
            .andExpect(content().string(not(containsString("5432"))))
            .andExpect(content().string(not(containsString("Exception"))))
            .andReturn();

    expectTraceIdMatchesHeader(result);
  }

  @Test
  void unexpectedErrorIsLoggedWithTheTraceId(CapturedOutput output) throws Exception {
    mvc.perform(get("/test/boom").header(TraceIdFilter.HEADER, "demo-trace-1"))
        .andExpect(status().isInternalServerError());

    assertThat(output.getOut()).contains("ERROR [demo-trace-1]");
  }

  private static ResultActions expectError(ResultActions actions, int status, String code)
      throws Exception {
    return actions
        .andExpect(status().is(status))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value(code));
  }

  private static void expectTraceIdMatchesHeader(MvcResult result) throws Exception {
    String traceId = result.getResponse().getHeader(TraceIdFilter.HEADER);
    assertThat(traceId).isNotBlank();
    jsonPath("$.traceId").value(traceId).match(result);
  }

  record Payload(@NotBlank String name) {}

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

    @GetMapping("/test/db")
    String db() {
      throw new DataAccessResourceFailureException("Connection to db:5432 refused");
    }

    @PostMapping("/test/body")
    String body(@Valid @RequestBody Payload payload) {
      return payload.name();
    }

    @GetMapping("/test/path/{id}")
    String path(@PathVariable @Size(max = 20) String id) {
      return id;
    }

    @GetMapping("/test/list")
    List<String> list(@RequestParam List<@Size(max = 20) String> skus) {
      return skus;
    }

    @GetMapping("/test/number")
    int number(@RequestParam int n) {
      return n;
    }
  }

  static class TestNotFoundException extends NotFoundException {
    TestNotFoundException(String message) {
      super("TEST_NOT_FOUND", message);
    }
  }
}
