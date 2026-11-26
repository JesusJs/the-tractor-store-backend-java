package com.tractorstore.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tractorstore.catalog.domain.port.out.StoreRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreExistsServiceTest {

  @Mock private StoreRepository stores;
  @InjectMocks private StoreExistsService service;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void delegatesToTheRepository(boolean exists) {
    when(stores.existsById("store-a")).thenReturn(exists);

    assertThat(service.exists("store-a")).isEqualTo(exists);
  }
}
