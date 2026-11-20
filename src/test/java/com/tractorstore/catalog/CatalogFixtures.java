package com.tractorstore.catalog;

import com.tractorstore.catalog.domain.Category;
import com.tractorstore.catalog.domain.Color;
import com.tractorstore.catalog.domain.Product;
import com.tractorstore.catalog.domain.Store;
import com.tractorstore.catalog.domain.Variant;
import java.math.BigDecimal;
import java.util.List;

/** Small catalog objects for unit and slice tests (values taken from the real dataset). */
public final class CatalogFixtures {

  public static final String CDN = "https://blueprint.the-tractor.store/cdn/img/";

  public static final Category CLASSIC =
      new Category("classic", "Classic", "Classic Tractors", CDN + "scene/500/classics.webp");
  public static final Category AUTONOMOUS =
      new Category(
          "autonomous", "Autonomous", "Autonomous Tractors", CDN + "scene/500/autonomous.webp");

  private CatalogFixtures() {}

  public static Variant variant(String sku, String color, String price) {
    return new Variant(
        sku,
        "Name " + sku,
        Color.fromHex(color),
        new BigDecimal(price),
        CDN + "product/400/" + sku + ".webp");
  }

  public static Product cl01() {
    return new Product(
        "CL-01",
        "Heritage Workhorse",
        CLASSIC,
        List.of(
            "Proven reliability with a touch of modern reliability enhancements",
            "Robust construction equipped to withstand decades of labor",
            "User-friendly operation with traditional manual controls"),
        List.of(
            variant("CL-01-GR", "#6B8E23", "5700.00"), variant("CL-01-GY", "#708090", "6200.00")));
  }

  public static Product au07() {
    return new Product(
        "AU-07",
        "Verde Voyager",
        AUTONOMOUS,
        List.of(
            "Adaptive drive system intelligently navigates through diverse field conditions",
            "Clean energy operation with advanced solar battery technology",
            "High-resolution field scanners for precise agronomy insights"),
        List.of(
            variant("AU-07-MT", "#AFDBD2", "4000.00"), variant("AU-07-YE", "#FFDA03", "5000.00")));
  }

  public static Store storeA() {
    return new Store(
        "store-a",
        "Aurora Flagship Store",
        "Astronaut Way 1",
        "Arlington",
        CDN + "store/400/store-1.webp");
  }
}
