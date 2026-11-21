package com.tractorstore.catalog.application.service;

import com.tractorstore.catalog.application.port.in.GetHomeTeasersUseCase;
import com.tractorstore.catalog.domain.model.Category;
import com.tractorstore.catalog.domain.port.out.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetHomeTeasersService implements GetHomeTeasersUseCase {

  private final CategoryRepository categories;

  GetHomeTeasersService(CategoryRepository categories) {
    this.categories = categories;
  }

  @Override
  public List<Category> get() {
    return categories.findAll();
  }
}
