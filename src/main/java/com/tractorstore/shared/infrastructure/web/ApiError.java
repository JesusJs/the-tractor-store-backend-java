package com.tractorstore.shared.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Single error body of the API (FR-014); {@code details} is omitted when there are none. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Map<String, String> details, String traceId) {}
