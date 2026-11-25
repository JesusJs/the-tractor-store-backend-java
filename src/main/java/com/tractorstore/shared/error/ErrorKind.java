package com.tractorstore.shared.error;

/**
 * Kind of business error, so domains never know HTTP status codes. Translated to HTTP in a single
 * place ({@code GlobalExceptionHandler}).
 */
public enum ErrorKind {
  /** Requested resource does not exist. */
  NOT_FOUND,
  /** Request conflicts with the current state (empty cart, out of stock). */
  CONFLICT
}
