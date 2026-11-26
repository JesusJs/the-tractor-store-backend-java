package com.tractorstore.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderIdTest {

  @Test
  void valueMustNotBeNull() {
    assertThatThrownBy(() -> new OrderId(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void newIdIsDifferentEachTime() {
    assertThat(OrderId.newId()).isNotEqualTo(OrderId.newId());
  }

  @Test
  void parsesACanonicalUuid() {
    String id = "0b8e6a8e-5d0f-4a51-9d3c-2f7f3f1a7c11";

    assertThat(OrderId.parse(id)).contains(new OrderId(UUID.fromString(id)));
  }

  @Test
  void parsesUppercaseAndPrintsLowercase() {
    String id = "0B8E6A8E-5D0F-4A51-9D3C-2F7F3F1A7C11";

    assertThat(OrderId.parse(id))
        .map(OrderId::toString)
        .contains("0b8e6a8e-5d0f-4a51-9d3c-2f7f3f1a7c11");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"1-1-1-1-1", "abc", " 0b8e6a8e-5d0f-4a51-9d3c-2f7f3f1a7c11"})
  void anythingElseIsEmpty(String raw) {
    assertThat(OrderId.parse(raw)).isEmpty();
  }
}
