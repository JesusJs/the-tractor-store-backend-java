package com.tractorstore.shared.error;

/** A request that conflicts with the current state (empty cart, no stock left...). */
public abstract class ConflictException extends BusinessException {

  protected ConflictException(String code, String message) {
    super(code, message);
  }

  @Override
  public ErrorKind kind() {
    return ErrorKind.CONFLICT;
  }
}
