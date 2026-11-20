package com.tractorstore.shared.infrastructure.web;

import com.tractorstore.shared.error.BusinessException;
import com.tractorstore.shared.error.ErrorKind;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The only place where errors become HTTP responses (FR-019). {@link ErrorKind#CONFLICT} is not
 * translated yet: spec 007 adds the 409; until then it falls into the generic 500 (research R8).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiError> handleBusiness(BusinessException error) {
    if (error.kind() == ErrorKind.NOT_FOUND) {
      return body(HttpStatus.NOT_FOUND, error.code(), error.getMessage(), null);
    }
    return unexpected(error);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> handleUnexpected(Exception error) throws Exception {
    if (error instanceof ErrorResponse) {
      // Spring MVC errors (404 route, 405...) keep Spring's default handling until US4 (T046).
      throw error;
    }
    return unexpected(error);
  }

  private static ResponseEntity<ApiError> unexpected(Exception error) {
    log.error("Unexpected error", error);
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", null);
  }

  private static ResponseEntity<ApiError> body(
      HttpStatus status, String code, String message, Map<String, String> details) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ApiError(code, message, details, MDC.get(TraceIdFilter.MDC_KEY)));
  }
}
