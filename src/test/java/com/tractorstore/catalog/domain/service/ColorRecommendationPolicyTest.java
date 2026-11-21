package com.tractorstore.catalog.domain.service;

import static com.tractorstore.catalog.CatalogFixtures.AUTONOMOUS;
import static com.tractorstore.catalog.CatalogFixtures.CLASSIC;
import static com.tractorstore.catalog.CatalogFixtures.variant;
import static org.assertj.core.api.Assertions.assertThat;

import com.tractorstore.catalog.domain.model.Category;
import com.tractorstore.catalog.domain.model.Product;
import com.tractorstore.catalog.domain.model.Recommendation;
import com.tractorstore.catalog.domain.model.Variant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test catalog with distances easy to compute by hand. Against grey {@code #808080}: AU-03-GY 1.7,
 * AU-01-SI 110.9, AU-02-WH 220.0, AU-02-RD 220.6, CL-01-RD 221.1 = CL-03-BL 221.1 (tie), CL-01-BK
 * 221.7.
 */
class ColorRecommendationPolicyTest {

  private static final List<Product> CATALOG =
      List.of(
          product(
              "CL-01",
              CLASSIC,
              variant("CL-01-RD", "#FF0000", "100.00"),
              variant("CL-01-BK", "#000000", "100.00")),
          product("CL-02", CLASSIC, variant("CL-02-GY", "#808080", "100.00")),
          product("AU-01", AUTONOMOUS, variant("AU-01-SI", "#C0C0C0", "100.00")),
          product(
              "AU-02",
              AUTONOMOUS,
              variant("AU-02-WH", "#FFFFFF", "100.00"),
              variant("AU-02-RD", "#FE0000", "100.00")),
          product("CL-03", CLASSIC, variant("CL-03-BL", "#0000FF", "100.00")),
          product("AU-03", AUTONOMOUS, variant("AU-03-GY", "#7F7F7F", "100.00")));

  private static final List<String> DEFAULT =
      List.of("CL-01-RD", "CL-02-GY", "AU-01-SI", "AU-02-WH");

  private final ColorRecommendationPolicy policy = new ColorRecommendationPolicy();

  private static Product product(String id, Category category, Variant... variants) {
    return new Product(id, "Name " + id, category, List.of("Highlight"), List.of(variants));
  }

  private List<String> recommendedSkus(Collection<String> requested) {
    return policy.recommend(CATALOG, requested).stream()
        .map(recommendation -> recommendation.variant().sku())
        .toList();
  }

  @Test
  void ordersByDistanceToTheAverageColorAndCutsAtTheLimit() {
    // CL-03-BL ties with CL-01-RD and loses on SKU, so the limit leaves it out.
    assertThat(recommendedSkus(List.of("CL-02-GY")))
        .containsExactly("AU-03-GY", "AU-01-SI", "AU-02-WH", "CL-01-RD");
    assertThat(ColorRecommendationPolicy.LIMIT).isEqualTo(4);
  }

  @Test
  void tieOnDistanceIsBrokenBySku() {
    List<Product> catalog =
        List.of(
            product("Z-0", CLASSIC, variant("Z-0-K", "#000000", "1.00")),
            product("P-2", CLASSIC, variant("P-2-A", "#010000", "1.00")),
            product("P-1", CLASSIC, variant("P-1-A", "#000100", "1.00")));

    assertThat(policy.recommend(catalog, List.of("Z-0-K")))
        .extracting(recommendation -> recommendation.variant().sku())
        .containsExactly("P-1-A", "P-2-A");
  }

  @Test
  void averageUsesEveryRequestedColor() {
    // Average of #FF0000 and #0000FF is (127.5, 0, 127.5): AU-03-GY (127.0) beats CL-02-GY (128.0);
    // using only the red one would pick AU-02-RD (distance 1).
    List<Recommendation> recommendations =
        policy.recommend(CATALOG, List.of("CL-01-RD", "CL-03-BL"));

    assertThat(recommendations).extracting(r -> r.variant().sku()).first().isEqualTo("AU-03-GY");
  }

  @Test
  void excludesRequestedSkusButKeepsTheirSiblings() {
    // From black: AU-03-GY 220.0, CL-02-GY 221.7, AU-02-RD 254.0, CL-01-RD 255.0 (beats CL-03-BL).
    List<String> skus = recommendedSkus(List.of("CL-01-BK"));

    assertThat(skus).doesNotContain("CL-01-BK").contains("CL-01-RD");
  }

  @Test
  void productWithAllItsVariantsRequestedDoesNotAppear() {
    assertThat(policy.recommend(CATALOG, List.of("CL-02-GY")))
        .extracting(r -> r.product().id())
        .doesNotContain("CL-02");
  }

  @Test
  void eachProductAppearsAtMostOnceWithItsClosestVariant() {
    // Against silver both AU-02 variants are candidates; white (109.1) beats red (278.0).
    List<Recommendation> recommendations = policy.recommend(CATALOG, List.of("AU-01-SI"));

    assertThat(recommendations).extracting(r -> r.product().id()).doesNotHaveDuplicates();
    assertThat(recommendations)
        .filteredOn(r -> r.product().id().equals("AU-02"))
        .extracting(r -> r.variant().sku())
        .containsExactly("AU-02-WH");
  }

  @Test
  void repeatedSkuCountsOnce() {
    assertThat(recommendedSkus(List.of("CL-01-RD", "CL-01-RD", "CL-01-RD", "AU-02-WH")))
        .isEqualTo(recommendedSkus(List.of("CL-01-RD", "AU-02-WH")));
  }

  @Test
  void blankSkusAreTrimmedOrIgnored() {
    assertThat(recommendedSkus(Arrays.asList(" CL-02-GY ", " ", "", null)))
        .isEqualTo(recommendedSkus(List.of("CL-02-GY")));
  }

  @Test
  void unknownSkusAreIgnored() {
    assertThat(recommendedSkus(List.of("CL-02-GY", "NOPE-00")))
        .isEqualTo(recommendedSkus(List.of("CL-02-GY")));
  }

  static Stream<Arguments> nothingKnownRequested() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(List.of()),
        Arguments.of(List.of("")),
        Arguments.of(List.of("NOPE-00")),
        Arguments.of(List.of(" ", "")));
  }

  @ParameterizedTest
  @MethodSource("nothingKnownRequested")
  @DisplayName("regresión: sin skus responde la lista por defecto, nunca error")
  void withoutKnownSkusReturnsTheDefaultList(Collection<String> requested) {
    List<Recommendation> recommendations = policy.recommend(CATALOG, requested);

    assertThat(recommendations).extracting(r -> r.variant().sku()).isEqualTo(DEFAULT);
    assertThat(recommendations)
        .allSatisfy(r -> assertThat(r.variant()).isEqualTo(r.product().variants().getFirst()));
  }

  @Test
  @DisplayName("regresión: recomienda por color y nunca por categoría")
  void recommendsByColorNotByCategory() {
    List<Recommendation> recommendations = policy.recommend(CATALOG, List.of("CL-02-GY"));

    assertThat(recommendations.getFirst().product().category()).isEqualTo(AUTONOMOUS);
    assertThat(recommendations.getFirst().variant().sku()).isEqualTo("AU-03-GY");
  }

  @Test
  void doesNotModifyTheRequestedCollection() {
    List<String> requested = new ArrayList<>(List.of(" CL-02-GY ", "CL-02-GY"));

    policy.recommend(CATALOG, requested);

    assertThat(requested).containsExactly(" CL-02-GY ", "CL-02-GY");
  }
}
