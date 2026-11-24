package com.tractorstore;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Negative proof (review 001 #4): every rule fails on a fixture that breaks it. */
class ArchitectureRulesSelfTests {

  static Stream<Arguments> violations() {
    return Stream.of(
        violation("domainspring", ArchitectureRules::domainIsFrameworkFree),
        violation("webrepo", ArchitectureRules::webDoesNotUsePersistence),
        violation("webport", ArchitectureRules::webDoesNotUsePersistence),
        violation("repoplace", ArchitectureRules::repositoriesLiveInPersistence),
        violation("coreinfra", ArchitectureRules::coreDoesNotDependOnInfrastructure),
        violation("apileak", ArchitectureRules::apiDoesNotExposeInternals),
        violation("entityplace", ArchitectureRules::entitiesLiveInPersistence),
        violation("sharedentity", ArchitectureRules::sharedHasNoEntities),
        violation("tableprefix", ArchitectureRules::tablesArePrefixedWithModule),
        violation("collectionprefix", ArchitectureRules::tablesArePrefixedWithModule),
        violation("portin", ArchitectureRules::useCasesLiveInPortIn),
        violation("portout", ArchitectureRules::domainRepositoriesLiveInPortOut));
  }

  private static Arguments violation(String fixture, Function<String, ArchRule> rule) {
    return Arguments.of(fixture, rule);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("violations")
  void ruleFailsOnItsFixture(String fixture, Function<String, ArchRule> rule) {
    String root = "archfixtures." + fixture;
    JavaClasses classes = new ClassFileImporter().importPackages(root);

    // A rule that matched nothing also throws; the fixture must produce a real violation.
    assertThatThrownBy(() -> rule.apply(root).check(classes))
        .isInstanceOf(AssertionError.class)
        .hasMessageNotContaining("failed to check any classes")
        .hasMessageContaining(root);
  }
}
