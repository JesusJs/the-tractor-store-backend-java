package com.tractorstore;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Architecture rules of the constitution (FR-004, FR-005, FR-028). Each rule is built from a root
 * package so {@code ArchitectureRulesSelfTests} can prove it fails on fixtures that live outside
 * {@code com.tractorstore} (where Spring would otherwise scan their
 * {@code @Entity}/{@code @Component}).
 */
public final class ArchitectureRules {

  public static final String ROOT = "com.tractorstore";

  public static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = domainIsFrameworkFree(ROOT);
  public static final ArchRule WEB_DOES_NOT_USE_PERSISTENCE = webDoesNotUsePersistence(ROOT);
  public static final ArchRule REPOSITORIES_LIVE_IN_PERSISTENCE =
      repositoriesLiveInPersistence(ROOT);
  public static final ArchRule CORE_DOES_NOT_DEPEND_ON_INFRASTRUCTURE =
      coreDoesNotDependOnInfrastructure(ROOT);
  public static final ArchRule API_DOES_NOT_EXPOSE_INTERNALS = apiDoesNotExposeInternals(ROOT);
  public static final ArchRule ENTITIES_LIVE_IN_PERSISTENCE = entitiesLiveInPersistence(ROOT);
  public static final ArchRule SHARED_HAS_NO_ENTITIES = sharedHasNoEntities(ROOT);
  public static final ArchRule TABLES_ARE_PREFIXED_WITH_MODULE = tablesArePrefixedWithModule(ROOT);
  public static final ArchRule USE_CASES_LIVE_IN_PORT_IN = useCasesLiveInPortIn(ROOT);
  public static final ArchRule DOMAIN_REPOSITORIES_LIVE_IN_PORT_OUT =
      domainRepositoriesLiveInPortOut(ROOT);

  private ArchitectureRules() {}

  static ArchRule domainIsFrameworkFree(String root) {
    return noClasses()
        .that()
        .resideInAnyPackage(root + "..domain..", root + ".shared.error..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "com.fasterxml.jackson..",
            "tools.jackson..",
            "org.hibernate..")
        .because("domain and shared errors must be plain Java (FR-004, ADR 0009)");
  }

  static ArchRule webDoesNotUsePersistence(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..infrastructure.web..")
        .should()
        .dependOnClassesThat(
            resideInAPackage(root + "..infrastructure.persistence..")
                .or(assignableTo(Repository.class)))
        .because("controllers must only call use cases (FR-005)");
  }

  static ArchRule repositoriesLiveInPersistence(String root) {
    return classes()
        .that()
        .resideInAPackage(root + "..")
        .and()
        .areAssignableTo(Repository.class)
        .should()
        .resideInAPackage(root + "..infrastructure.persistence..")
        .because("Spring Data repositories are persistence adapters (FR-005)");
  }

  static ArchRule coreDoesNotDependOnInfrastructure(String root) {
    return noClasses()
        .that()
        .resideInAnyPackage(root + "..domain..", root + "..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(root + "..infrastructure..")
        .because("dependencies point inwards: adapters depend on the core, never the reverse");
  }

  /** Empty allowed only here: {@code catalog.api} stays empty until spec 007 (research R11). */
  static ArchRule apiDoesNotExposeInternals(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..api..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            root + "..domain..", root + "..application..", root + "..infrastructure..")
        .because("other modules may only see the api package (review 001 #7)")
        .allowEmptyShould(true);
  }

  static ArchRule entitiesLiveInPersistence(String root) {
    return classes()
        .that()
        .areAnnotatedWith(Entity.class)
        .should()
        .resideInAPackage(root + ".*.infrastructure.persistence..")
        .because("JPA entities are persistence details and never leave their adapter");
  }

  static ArchRule sharedHasNoEntities(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + ".shared..")
        .should()
        .beAnnotatedWith(Entity.class)
        .because("the shared technical module owns no tables (constitution II, ADR 0009)");
  }

  static ArchRule tablesArePrefixedWithModule(String root) {
    return classes()
        .that()
        .areAnnotatedWith(Entity.class)
        .and()
        .resideInAPackage(root + "..")
        .should(haveTablesPrefixedWithTheirModule(root))
        .because("regresión .NET: entidades duplicadas entre módulos");
  }

  static ArchRule useCasesLiveInPortIn(String root) {
    return classes()
        .that()
        .resideInAPackage(root + "..application..")
        .and()
        .areInterfaces()
        .and()
        .doNotHaveSimpleName("package-info")
        .should()
        .resideInAPackage(root + "..application.port.in..")
        .because("inbound ports (use case interfaces) live in application.port.in (ADR 0001)");
  }

  static ArchRule domainRepositoriesLiveInPortOut(String root) {
    return classes()
        .that()
        .resideInAPackage(root + "..domain..")
        .and()
        .haveSimpleNameEndingWith("Repository")
        .should()
        .resideInAPackage(root + "..domain.port.out..")
        .because("outbound ports (domain repositories) live in domain.port.out (ADR 0001)");
  }

  private static ArchCondition<JavaClass> haveTablesPrefixedWithTheirModule(String root) {
    return new ArchCondition<>("map only tables prefixed with their module") {
      @Override
      public void check(JavaClass entity, ConditionEvents events) {
        String prefix = moduleOf(root, entity) + "_";
        String table = entity.tryGetAnnotationOfType(Table.class).map(Table::name).orElse("");
        requirePrefix(entity, "@Table", table, prefix, events);
        for (JavaField field : entity.getFields()) {
          tableName(field, CollectionTable.class)
              .ifPresent(name -> requirePrefix(entity, "@CollectionTable", name, prefix, events));
          tableName(field, JoinTable.class)
              .ifPresent(name -> requirePrefix(entity, "@JoinTable", name, prefix, events));
        }
      }
    };
  }

  private static Optional<String> tableName(JavaField field, Class<?> annotation) {
    if (annotation == CollectionTable.class) {
      return field.tryGetAnnotationOfType(CollectionTable.class).map(CollectionTable::name);
    }
    return field.tryGetAnnotationOfType(JoinTable.class).map(JoinTable::name);
  }

  private static void requirePrefix(
      JavaClass entity, String what, String table, String prefix, ConditionEvents events) {
    if (!table.startsWith(prefix)) {
      events.add(
          SimpleConditionEvent.violated(
              entity,
              "%s %s maps table '%s', expected prefix '%s'"
                  .formatted(entity.getName(), what, table, prefix)));
    }
  }

  /** {@code com.tractorstore.catalog.infrastructure...} → {@code catalog}. */
  private static String moduleOf(String root, JavaClass javaClass) {
    String relative = javaClass.getPackageName().substring(root.length() + 1);
    int dot = relative.indexOf('.');
    return dot < 0 ? relative : relative.substring(0, dot);
  }
}
