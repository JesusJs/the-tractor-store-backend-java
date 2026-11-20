package com.tractorstore.catalog.application;

import static com.tractorstore.catalog.CatalogFixtures.AUTONOMOUS;
import static com.tractorstore.catalog.CatalogFixtures.CLASSIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.domain.port.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetHomeTeasersServiceTest {

  @Mock private CategoryRepository categories;
  @InjectMocks private GetHomeTeasersService service;

  @Test
  void returnsTheCategoriesInDatasetOrder() {
    when(categories.findAll()).thenReturn(List.of(CLASSIC, AUTONOMOUS));

    assertThat(service.get()).containsExactly(CLASSIC, AUTONOMOUS);
  }
}
