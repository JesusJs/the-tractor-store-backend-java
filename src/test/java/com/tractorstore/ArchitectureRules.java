package com.tractorstore;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Table;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Architecture rules of the constitution (FR-004, FR-005, FR-028..FR-030). Each rule is built from
 * a root package so {@code ArchitectureRulesSelfTests} can prove it fails on fixtures that live
 * outside {@code com.tractorstore} (where Spring would otherwise scan their
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
  public static final ArchRule DOMAIN_PORTS_LIVE_IN_PORT_OUT = domainPortsLiveInPortOut(ROOT);
  public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_APPLICATION =
      domainDoesNotDependOnApplication(ROOT);
  public static final ArchRule APPLICATION_USES_ONLY_SERVICE_AND_TRANSACTIONAL =
      applicationUsesOnlyServiceAndTransactional(ROOT);
  public static final ArchRule TRANSACTIONAL_ONLY_IN_APPLICATION =
      transactionalOnlyInApplication(ROOT);

  private ArchitectureRules() {}

  static ArchRule domainIsFrameworkFree(String root) {
    return noClasses()
        .that()
        .resideInAnyPackage(root + "..domain..", root + ".shared.error..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta..",
            "com.fasterxml.jackson..",
            "tools.jackson..",
            "org.hibernate..",
            "io.swagger..")
        .because("domain and shared errors must be plain Java (FR-004, FR-029, ADR 0009)");
  }

  static ArchRule webDoesNotUsePersistence(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..infrastructure.web..")
        .should()
        .dependOnClassesThat(
            resideInAnyPackage(
                    root + "..infrastructure.persistence..", root + "..domain.port.out..")
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

  /**
   * Every domain interface is an outbound port, and so is any {@code *Repository}/{@code *Gateway}.
   */
  static ArchRule domainPortsLiveInPortOut(String root) {
    return classes()
        .that()
        .resideInAPackage(root + "..domain..")
        .and(INTERFACES.or(simpleNameEndingWith("Repository")).or(simpleNameEndingWith("Gateway")))
        .and()
        .doNotHaveSimpleName("package-info")
        .should()
        .resideInAPackage(root + "..domain.port.out..")
        .because("outbound ports live in domain.port.out (ADR 0001, FR-029)");
  }

  static ArchRule domainDoesNotDependOnApplication(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(root + "..application..")
        .because("the domain is the innermost layer (FR-030)");
  }

  static ArchRule applicationUsesOnlyServiceAndTransactional(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..application..")
        .should()
        .dependOnClassesThat(
            resideInAPackage("org.springframework..")
                .and(
                    not(
                        belongToAnyOf(
                            Service.class,
                            Transactional.class,
                            Propagation.class,
                            Isolation.class))))
        .because("use cases only declare their bean and transaction boundary (FR-030)");
  }

  /**
   * Only a <em>direct</em> annotation violates: {@code @ApplicationModuleListener} is
   * meta-annotated with {@code @Transactional} and is allowed in infrastructure (research R16).
   */
  static ArchRule transactionalOnlyInApplication(String root) {
    String spring = "org.springframework.transaction.annotation.Transactional";
    String jakarta = "jakarta.transaction.Transactional";
    String because = "the transaction boundary is the use case (FR-030)";
    ArchRule onClasses =
        noClasses()
            .that()
            .resideInAPackage(root + "..")
            .and()
            .resideOutsideOfPackage(root + "..application..")
            .should()
            .beAnnotatedWith(spring)
            .orShould()
            .beAnnotatedWith(jakarta)
            .because(because);
    ArchRule onMethods =
        noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage(root + "..")
            .and()
            .areDeclaredInClassesThat()
            .resideOutsideOfPackage(root + "..application..")
            .should()
            .beAnnotatedWith(spring)
            .orShould()
            .beAnnotatedWith(jakarta)
            .because(because);
    return CompositeArchRule.of(onClasses).and(onMethods);
  }

  private static ArchCondition<JavaClass> haveTablesPrefixedWithTheirModule(String root) {
    return new ArchCondition<>("map only tables prefixed with their module") {
      @Override
      public void check(JavaClass entity, ConditionEvents events) {
        String prefix = moduleOf(root, entity) + "_";
        String table = entity.tryGetAnnotationOfType(Table.class).map(Table::name).orElse("");
        requirePrefix(entity, "@Table", table, prefix, events);
        entity
            .tryGetAnnotationOfType(SecondaryTable.class)
            .ifPresent(t -> requirePrefix(entity, "@SecondaryTable", t.name(), prefix, events));
        entity
            .tryGetAnnotationOfType(SecondaryTables.class)
            .ifPresent(
                all -> {
                  for (SecondaryTable t : all.value()) {
                    requirePrefix(entity, "@SecondaryTable", t.name(), prefix, events);
                  }
                });
        for (JavaField field : entity.getFields()) {
          checkCollectionTable(entity, field, prefix, events);
          checkJoinTable(entity, field, prefix, events);
        }
      }
    };
  }

  /** A default collection table is named {@code <entity>_<field>}, never with the prefix. */
  private static void checkCollectionTable(
      JavaClass entity, JavaField field, String prefix, ConditionEvents events) {
    field
        .tryGetAnnotationOfType(CollectionTable.class)
        .ifPresentOrElse(
            t -> requirePrefix(entity, "@CollectionTable", t.name(), prefix, events),
            () -> {
              if (field.isAnnotatedWith(ElementCollection.class)) {
                missing(entity, field, "@ElementCollection", "@CollectionTable", events);
              }
            });
  }

  /**
   * Only the owning side ({@code mappedBy} empty) of a {@code @ManyToMany} names the table. A
   * unidirectional {@code @OneToMany} without {@code @JoinColumn} also gets a default join table.
   */
  private static void checkJoinTable(
      JavaClass entity, JavaField field, String prefix, ConditionEvents events) {
    field
        .tryGetAnnotationOfType(JoinTable.class)
        .ifPresentOrElse(
            t -> requirePrefix(entity, "@JoinTable", t.name(), prefix, events),
            () -> {
              boolean owningManyToMany =
                  field
                      .tryGetAnnotationOfType(ManyToMany.class)
                      .map(m -> m.mappedBy().isEmpty())
                      .orElse(false);
              if (owningManyToMany) {
                missing(entity, field, "@ManyToMany", "@JoinTable", events);
              }
              boolean defaultOneToManyJoinTable =
                  field
                          .tryGetAnnotationOfType(OneToMany.class)
                          .map(m -> m.mappedBy().isEmpty())
                          .orElse(false)
                      && !field.isAnnotatedWith(JoinColumn.class)
                      && !field.isAnnotatedWith(JoinColumns.class);
              if (defaultOneToManyJoinTable) {
                missing(entity, field, "@OneToMany without @JoinColumn", "@JoinTable", events);
              }
            });
  }

  private static void missing(
      JavaClass entity, JavaField field, String mapping, String table, ConditionEvents events) {
    events.add(
        SimpleConditionEvent.violated(
            entity,
            "%s.%s is %s without explicit %s"
                .formatted(entity.getName(), field.getName(), mapping, table)));
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
