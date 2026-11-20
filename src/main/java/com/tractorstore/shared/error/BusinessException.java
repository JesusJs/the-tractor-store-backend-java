package com.tractorstore.shared.error;

/** Base of every business error: a stable {@code code} for the API and its {@link ErrorKind}. */
public abstract class BusinessException extends RuntimeException {

  private final String code;

  protected BusinessException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }

  public abstract ErrorKind kind();
}
