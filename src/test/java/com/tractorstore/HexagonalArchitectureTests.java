package com.tractorstore;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tractorstore", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTests {

  @ArchTest
  static final ArchRule domainIsFrameworkFree =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "com.fasterxml.jackson..",
              "tools.jackson..",
              "org.hibernate..")
          .because("domain must be plain Java (FR-004)")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule webDoesNotUsePersistence =
      noClasses()
          .that()
          .resideInAPackage("..infrastructure.web..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..infrastructure.persistence..", "org.springframework.data.repository..")
          .because("controllers must only call use cases (FR-005)")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule coreDoesNotDependOnInfrastructure =
      noClasses()
          .that()
          .resideInAnyPackage("..domain..", "..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..infrastructure..")
          .because("dependencies point inwards: adapters depend on the core, never the reverse")
          .allowEmptyShould(true);
}
