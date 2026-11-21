package com.tractorstore.catalog.domain.exception;

import com.tractorstore.shared.error.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

  public CategoryNotFoundException(String filter) {
    super("CATEGORY_NOT_FOUND", "Category " + filter + " not found");
  }
}
