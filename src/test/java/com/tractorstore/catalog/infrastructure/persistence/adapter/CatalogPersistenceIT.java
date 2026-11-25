package com.tractorstore.catalog.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.tractorstore.TestcontainersConfiguration;
import com.tractorstore.catalog.domain.model.Category;
import com.tractorstore.catalog.domain.model.Product;
import com.tractorstore.catalog.domain.model.Store;
import com.tractorstore.catalog.domain.model.Variant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Catalog read from the real Flyway migrations on an empty PostgreSQL (US5). */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  TestcontainersConfiguration.class,
  ProductRepositoryAdapter.class,
  CategoryRepositoryAdapter.class,
  StoreRepositoryAdapter.class
})
@ActiveProfiles("test")
@DisplayName("regresión: el catálogo sale de migraciones reales, no de datos en memoria")
class CatalogPersistenceIT {

  private static final Logger log = LoggerFactory.getLogger(CatalogPersistenceIT.class);
  private static final String CDN = "https://blueprint.the-tractor.store/cdn/img/";

  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManager entityManager;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private Flyway flyway;
  @Autowired private Environment environment;
  @Autowired private ProductRepositoryAdapter products;
  @Autowired private CategoryRepositoryAdapter categories;
  @Autowired private StoreRepositoryAdapter stores;

  private Statistics statistics;

  @BeforeEach
  void setUp() {
    statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
  }

  @Test
  void datasetHasTheOfficialCounts() {
    assertThat(counts()).containsExactly(23, 47, 69, 4, 2);
    log.info("catalog counts products|variants|highlights|stores|categories = {}", counts());
  }

  @Test
  void findAllKeepsDatasetOrder() {
    List<Product> all = products.findAll();

    assertThat(all).hasSize(23);
    assertThat(all.getFirst().id()).isEqualTo("CL-01");
    assertThat(all.getLast().id()).isEqualTo("AU-08");
    assertThat(all.subList(14, 16)).extracting(Product::id).containsExactly("CL-15", "AU-01");
  }

  @Test
  void findByCategoryFiltersAndKeepsOrder() {
    assertThat(products.findByCategory("classic"))
        .extracting(Product::id)
        .hasSize(15)
        .startsWith("CL-01")
        .endsWith("CL-15")
        .allMatch(id -> id.startsWith("CL-"));
    assertThat(products.findByCategory("autonomous"))
        .extracting(Product::id)
        .hasSize(8)
        .startsWith("AU-01")
        .endsWith("AU-08");
    assertThat(products.findByCategory("tractors")).isEmpty();
  }

  @Test
  void au01HasItsHighlightsAsIs() {
    Product au01 = products.findById("AU-01").orElseThrow();

    // The blueprint misspells the key as "highlightsa"; V3 loads them as highlights.
    assertThat(au01.highlights())
        .containsExactly(
            "Precision GPS mapping optimizes field coverage.",
            "Hybrid engine ensures eco-friendly extended operation.",
            "Fully autonomous with smart obstacle detection and terrain adaptation.");
    assertThat(au01.skus()).containsExactly("AU-01-SI");
    assertThat(au01.category().code()).isEqualTo("autonomous");
    assertThat(au01.brand()).isEqualTo("Autonomous");
  }

  @Test
  void au07HasVariantsInOrderAndFromPrice() {
    Product au07 = products.findById("AU-07").orElseThrow();

    assertThat(au07.name()).isEqualTo("Verde Voyager");
    assertThat(au07.skus()).containsExactly("AU-07-MT", "AU-07-YE");
    assertThat(au07.startPrice()).isEqualByComparingTo("4000.00");
    assertThat(au07.startPrice().scale()).isEqualTo(2);
    assertThat(au07.mainImage()).isEqualTo(CDN + "product/400/AU-07-MT.webp");
  }

  @Test
  void cl01MatchesTheContractExample() {
    Product cl01 = products.findById("CL-01").orElseThrow();

    assertThat(cl01.name()).isEqualTo("Heritage Workhorse");
    assertThat(cl01.skus()).containsExactly("CL-01-GR", "CL-01-GY");
    assertThat(cl01.startPrice()).isEqualByComparingTo("5700.00");
    assertThat(cl01.description())
        .isEqualTo(
            "Proven reliability with a touch of modern reliability enhancements. Robust"
                + " construction equipped to withstand decades of labor. User-friendly operation"
                + " with traditional manual controls");
  }

  @Test
  void unknownProductIsEmpty() {
    assertThat(products.findById("CL-99")).isEmpty();
  }

  @Test
  void categoriesKeepDatasetOrder() {
    assertThat(categories.findAll())
        .extracting(Category::code, Category::brandLabel, Category::teaserTitle)
        .containsExactly(
            tuple("classic", "Classic", "Classic Tractors"),
            tuple("autonomous", "Autonomous", "Autonomous Tractors"));
    assertThat(categories.findByCode("classic")).isPresent();
    assertThat(categories.findByCode("Classic")).isEmpty();
  }

  @Test
  void storesKeepDatasetOrder() {
    List<Store> all = stores.findAll();

    assertThat(all)
        .extracting(Store::id)
        .containsExactly("store-a", "store-b", "store-c", "store-d");
    assertThat(all.getFirst().address()).isEqualTo("Astronaut Way 1");
    assertThat(all.getFirst().city()).isEqualTo("Arlington");
    assertThat(all.getFirst().image()).isEqualTo(CDN + "store/400/store-1.webp");
  }

  @Test
  void everyImageIsAnAbsoluteCdnUrl() {
    Stream<String> images =
        Stream.of(
                products.findAll().stream()
                    .flatMap(product -> product.variants().stream())
                    .map(Variant::image),
                categories.findAll().stream().map(Category::teaserImage),
                stores.findAll().stream().map(Store::image))
            .flatMap(stream -> stream);

    assertThat(images).hasSize(47 + 2 + 4).allMatch(image -> image.startsWith(CDN));
  }

  @Test
  void productDetailIsOneSqlStatement() {
    long statements = statementsFor(() -> products.findById("AU-07").orElseThrow());

    log.info("SQL statements for product detail AU-07 = {}", statements);
    assertThat(statements).isEqualTo(1);
  }

  @Test
  void listingsAreOneSqlStatement() {
    long all = statementsFor(products::findAll);
    long classic = statementsFor(() -> products.findByCategory("classic"));

    log.info("SQL statements for listing all = {}, classic = {}", all, classic);
    assertThat(all).isEqualTo(1);
    assertThat(classic).isEqualTo(1);
  }

  @Test
  void findByVariantSkusLoadsWholeProductsInOneSqlStatement() {
    List<Product> found = products.findByVariantSkus(List.of("AU-07-YE", "CL-01-GY"));

    assertThat(found)
        .extracting(Product::id, Product::skus)
        .containsExactlyInAnyOrder(
            tuple("AU-07", List.of("AU-07-MT", "AU-07-YE")),
            tuple("CL-01", List.of("CL-01-GR", "CL-01-GY")));
    long statements =
        statementsFor(() -> products.findByVariantSkus(List.of("AU-07-YE", "CL-01-GY")));
    log.info("SQL statements for findByVariantSkus = {}", statements);
    assertThat(statements).isEqualTo(1);
  }

  @Test
  void findByVariantSkusIgnoresUnknownSkus() {
    assertThat(products.findByVariantSkus(List.of("XX-99-ZZ"))).isEmpty();
  }

  @Test
  void storeExistsComparesTheIdExactly() {
    assertThat(stores.existsById("store-a")).isTrue();
    assertThat(stores.existsById("store-x")).isFalse();
    assertThat(stores.existsById("Store-A")).isFalse();
  }

  @Test
  void migratingAgainChangesNothing() {
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(counts()).containsExactly(23, 47, 69, 4, 2);
  }

  @Test
  void schemaIsOnlyValidatedByHibernate() {
    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
  }

  private long statementsFor(Supplier<?> read) {
    entityManager.clear();
    statistics.clear();
    read.get();
    return statistics.getPrepareStatementCount();
  }

  private List<Integer> counts() {
    return Stream.of(
            "catalog_product",
            "catalog_variant",
            "catalog_product_highlight",
            "catalog_store",
            "catalog_category")
        .map(table -> jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class))
        .toList();
  }
}
