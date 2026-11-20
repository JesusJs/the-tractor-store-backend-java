package com.tractorstore.catalog.application;

import static com.tractorstore.catalog.CatalogFixtures.CDN;
import static com.tractorstore.catalog.CatalogFixtures.storeA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.domain.Store;
import com.tractorstore.catalog.domain.port.StoreRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListStoresServiceTest {

  @Mock private StoreRepository stores;
  @InjectMocks private ListStoresService service;

  @Test
  void returnsTheStoresInDatasetOrder() {
    Store storeB =
        new Store(
            "store-b",
            "Big Micro Machines",
            "Broadway 2",
            "Burlington",
            CDN + "store/400/store-2.webp");
    when(stores.findAll()).thenReturn(List.of(storeA(), storeB));

    assertThat(service.list()).containsExactly(storeA(), storeB);
  }
}
