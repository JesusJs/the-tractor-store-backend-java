package com.tractorstore.shared.error;

/** A business resource that does not exist. */
public abstract class NotFoundException extends BusinessException {

  protected NotFoundException(String code, String message) {
    super(code, message);
  }

  @Override
  public ErrorKind kind() {
    return ErrorKind.NOT_FOUND;
  }
}
