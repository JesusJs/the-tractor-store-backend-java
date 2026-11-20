package com.tractorstore.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductTest {

  private static final String CDN = "https://blueprint.the-tractor.store/cdn/img/";
  private static final Category CLASSIC =
      new Category("classic", "Classic", "Classic Tractors", CDN + "scene/500/classics.webp");
  private static final Category AUTONOMOUS =
      new Category(
          "autonomous", "Autonomous", "Autonomous Tractors", CDN + "scene/500/autonomous.webp");

  private static Variant variant(String sku, String color, String price) {
    return new Variant(sku, "Name " + sku, Color.fromHex(color), new BigDecimal(price), image(sku));
  }

  private static String image(String sku) {
    return CDN + "product/400/" + sku + ".webp";
  }

  private static Product cl01() {
    return new Product(
        "CL-01",
        "Heritage Workhorse",
        CLASSIC,
        List.of("A", "B", "C"),
        List.of(
            variant("CL-01-GR", "#6B8E23", "5700.00"), variant("CL-01-GY", "#808080", "6200.00")));
  }

  private static Product withHighlights(List<String> highlights) {
    return new Product(
        "AU-01",
        "TerraFirma AutoCultivator T-300",
        AUTONOMOUS,
        highlights,
        List.of(variant("AU-01-SI", "#C0C0C0", "1000.00")));
  }

  @Test
  void startPriceIsTheLowestVariantPrice() {
    Product product =
        new Product(
            "CL-01",
            "Heritage Workhorse",
            CLASSIC,
            List.of("A"),
            List.of(
                variant("CL-01-GY", "#808080", "6200.00"),
                variant("CL-01-GR", "#6B8E23", "5700.00")));

    assertThat(product.startPrice()).isEqualByComparingTo("5700.00");
  }

  @Test
  void mainImageIsTheFirstVariantImage() {
    assertThat(cl01().mainImage()).isEqualTo(image("CL-01-GR"));
  }

  @Test
  void skusKeepDatasetOrder() {
    assertThat(cl01().skus()).containsExactly("CL-01-GR", "CL-01-GY");
  }

  @Test
  void brandIsTheCategoryLabel() {
    assertThat(cl01().brand()).isEqualTo("Classic");
    assertThat(withHighlights(List.of("A")).brand()).isEqualTo("Autonomous");
  }

  @Test
  void descriptionJoinsHighlights() {
    assertThat(withHighlights(List.of("A", "B")).description()).isEqualTo("A. B");
  }

  @Test
  void descriptionDropsOneTrailingDotPerHighlight() {
    assertThat(withHighlights(List.of("A.", "B")).description()).isEqualTo("A. B");
  }

  @Test
  void descriptionOfAu01MatchesResearchR4() {
    Product au01 =
        withHighlights(
            List.of(
                "Precision GPS mapping optimizes field coverage.",
                "Hybrid engine ensures eco-friendly extended operation.",
                "Fully autonomous with smart obstacle detection and terrain adaptation."));

    assertThat(au01.description())
        .isEqualTo(
            "Precision GPS mapping optimizes field coverage. Hybrid engine ensures eco-friendly"
                + " extended operation. Fully autonomous with smart obstacle detection and terrain"
                + " adaptation");
  }

  @Test
  void highlightsAreReturnedAsIs() {
    assertThat(withHighlights(List.of("A.", "B")).highlights()).containsExactly("A.", "B");
  }

  @Test
  void skusStartingWithPutsTheGivenSkuFirst() {
    assertThat(cl01().skusStartingWith("CL-01-GY")).containsExactly("CL-01-GY", "CL-01-GR");
    assertThat(cl01().skusStartingWith("CL-01-GR")).containsExactly("CL-01-GR", "CL-01-GY");
  }

  @Test
  void listsAreImmutableCopies() {
    List<String> highlights = new ArrayList<>(List.of("A"));
    List<Variant> variants = new ArrayList<>(List.of(variant("CL-01-GR", "#6B8E23", "5700.00")));
    Product product = new Product("CL-01", "Heritage Workhorse", CLASSIC, highlights, variants);

    highlights.add("B");
    variants.add(variant("CL-01-GY", "#808080", "6200.00"));

    assertThat(product.highlights()).containsExactly("A");
    assertThat(product.variants()).hasSize(1);
    assertThatThrownBy(() -> product.highlights().add("C"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> product.variants().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void productRejectsBlankIdOrName(String blank) {
    List<Variant> variants = List.of(variant("CL-01-GR", "#6B8E23", "5700.00"));

    assertThatThrownBy(() -> new Product(blank, "Name", CLASSIC, List.of(), variants))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Product("CL-01", blank, CLASSIC, List.of(), variants))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void productNeedsAtLeastOneVariant() {
    assertThatThrownBy(() -> new Product("CL-01", "Name", CLASSIC, List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void variantRejectsBlankSkuOrName(String blank) {
    Color color = Color.fromHex("#C0C0C0");
    BigDecimal price = new BigDecimal("1000.00");

    assertThatThrownBy(() -> new Variant(blank, "Silver", color, price, image("X")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Variant("AU-01-SI", blank, color, price, image("X")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "0.00", "-1.00"})
  void variantRejectsNonPositivePrice(String price) {
    Color color = Color.fromHex("#C0C0C0");

    assertThatThrownBy(
            () -> new Variant("AU-01-SI", "Silver", color, new BigDecimal(price), image("X")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void categoryRejectsBlankFields(String blank) {
    assertThatThrownBy(() -> new Category(blank, "Classic", "Classic Tractors", "img"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Category("classic", blank, "Classic Tractors", "img"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Category("classic", "Classic", blank, "img"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Category("classic", "Classic", "Classic Tractors", blank))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
