package com.tractorstore.catalog.application;

import com.tractorstore.catalog.domain.Category;
import com.tractorstore.catalog.domain.port.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetHomeTeasersService implements GetHomeTeasers {

  private final CategoryRepository categories;

  GetHomeTeasersService(CategoryRepository categories) {
    this.categories = categories;
  }

  @Override
  public List<Category> get() {
    return categories.findAll();
  }
}
