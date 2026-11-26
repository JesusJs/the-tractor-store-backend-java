package com.tractorstore;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Applies {@link ArchitectureRules} to the production code; their self-tests prove they bite. */
@AnalyzeClasses(packages = "com.tractorstore", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTests {

  @ArchTest
  static final ArchRule domainIsFrameworkFree = ArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE;

  @ArchTest
  static final ArchRule webDoesNotUsePersistence = ArchitectureRules.WEB_DOES_NOT_USE_PERSISTENCE;

  @ArchTest
  static final ArchRule repositoriesLiveInPersistence =
      ArchitectureRules.REPOSITORIES_LIVE_IN_PERSISTENCE;

  @ArchTest
  static final ArchRule coreDoesNotDependOnInfrastructure =
      ArchitectureRules.CORE_DOES_NOT_DEPEND_ON_INFRASTRUCTURE;

  @ArchTest
  static final ArchRule apiDoesNotExposeInternals = ArchitectureRules.API_DOES_NOT_EXPOSE_INTERNALS;

  @ArchTest
  static final ArchRule entitiesLiveInPersistence = ArchitectureRules.ENTITIES_LIVE_IN_PERSISTENCE;

  @ArchTest static final ArchRule sharedHasNoEntities = ArchitectureRules.SHARED_HAS_NO_ENTITIES;

  @ArchTest
  static final ArchRule tablesArePrefixedWithModule =
      ArchitectureRules.TABLES_ARE_PREFIXED_WITH_MODULE;

  @ArchTest
  static final ArchRule useCasesLiveInPortIn = ArchitectureRules.USE_CASES_LIVE_IN_PORT_IN;

  @ArchTest
  static final ArchRule domainPortsLiveInPortOut = ArchitectureRules.DOMAIN_PORTS_LIVE_IN_PORT_OUT;

  @ArchTest
  static final ArchRule domainDoesNotDependOnApplication =
      ArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_APPLICATION;

  @ArchTest
  static final ArchRule applicationUsesOnlyServiceAndTransactional =
      ArchitectureRules.APPLICATION_USES_ONLY_SERVICE_AND_TRANSACTIONAL;

  @ArchTest
  static final ArchRule transactionalOnlyInApplication =
      ArchitectureRules.TRANSACTIONAL_ONLY_IN_APPLICATION;
}
