package com.tractorstore.shared.infrastructure.web;

import com.tractorstore.shared.error.BusinessException;
import com.tractorstore.shared.error.ErrorKind;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The only place where errors become HTTP responses (FR-014, research R8). {@link
 * ErrorKind#CONFLICT} is not translated yet: spec 007 adds the 409; until then it falls into the
 * generic 500.
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

  /** {@code @Valid @RequestBody} with constraint violations. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException error) {
    Map<String, String> details = new LinkedHashMap<>();
    error
        .getBindingResult()
        .getFieldErrors()
        .forEach(field -> details.putIfAbsent(field.getField(), field.getDefaultMessage()));
    return validationError(details);
  }

  /** Constraints on {@code @PathVariable}/{@code @RequestParam} (Spring MVC method validation). */
  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ApiError> handleInvalidParameters(HandlerMethodValidationException error) {
    Map<String, String> details = new LinkedHashMap<>();
    error
        .getParameterValidationResults()
        .forEach(
            result ->
                result
                    .getResolvableErrors()
                    .forEach(
                        violation ->
                            details.putIfAbsent(
                                result.getMethodParameter().getParameterName(),
                                violation.getDefaultMessage())));
    return validationError(details);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<ApiError> handleMalformed(Exception error) {
    log.debug("Malformed request: {}", error.getMessage());
    return body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request", null);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> handleNoRoute(NoResourceFoundException error) {
    return body(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException error) {
    return body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed", null);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> handleUnexpected(Exception error) {
    return unexpected(error);
  }

  private static ResponseEntity<ApiError> validationError(Map<String, String> details) {
    return body(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "Request validation failed",
        details.isEmpty() ? null : details);
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
