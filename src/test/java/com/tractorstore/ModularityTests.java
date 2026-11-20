package com.tractorstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

  private final ApplicationModules modules = ApplicationModules.of(TractorStoreApplication.class);

  @Test
  void modulesRespectTheirBoundaries() {
    modules.verify();
  }

  @Test
  void detectsTheFiveBusinessModulesPlusShared() {
    assertThat(modules.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
        .containsExactlyInAnyOrder(
            "catalog", "inventory", "cart", "order", "notifications", "shared");
  }

  @Test
  void writesModuleDocumentation() {
    new Documenter(modules).writeDocumentation();
  }
}
