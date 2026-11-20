package com.tractorstore.catalog.domain;

import static com.tractorstore.catalog.domain.DomainChecks.requireText;
import static com.tractorstore.catalog.domain.DomainChecks.requireValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Tractor of the catalog with its variants in dataset order. Immutable. */
public final class Product {

  private final String id;
  private final String name;
  private final Category category;
  private final List<String> highlights;
  private final List<Variant> variants;

  public Product(
      String id, String name, Category category, List<String> highlights, List<Variant> variants) {
    this.id = requireText(id, "id");
    this.name = requireText(name, "name");
    this.category = requireValue(category, "category");
    this.highlights = List.copyOf(requireValue(highlights, "highlights"));
    this.variants = List.copyOf(requireValue(variants, "variants"));
    if (this.variants.isEmpty()) {
      throw new IllegalArgumentException("product " + id + " needs at least one variant");
    }
  }

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Category category() {
    return category;
  }

  public List<String> highlights() {
    return highlights;
  }

  public List<Variant> variants() {
    return variants;
  }

  /** "From" price: the lowest variant price (FR-003). */
  public BigDecimal startPrice() {
    return variants.stream().map(Variant::price).min(Comparator.naturalOrder()).orElseThrow();
  }

  public String mainImage() {
    return variants.getFirst().image();
  }

  public List<String> skus() {
    return variants.stream().map(Variant::sku).toList();
  }

  public String brand() {
    return category.brandLabel();
  }

  /** Highlights joined with {@code ". "}, dropping one trailing dot from each (research R4). */
  public String description() {
    return highlights.stream()
        .map(text -> text.endsWith(".") ? text.substring(0, text.length() - 1) : text)
        .collect(Collectors.joining(". "));
  }

  /** SKUs with the given one first and the rest in dataset order. */
  public List<String> skusStartingWith(String sku) {
    List<String> ordered = new ArrayList<>(skus().size());
    ordered.add(sku);
    skus().stream().filter(other -> !other.equals(sku)).forEach(ordered::add);
    return List.copyOf(ordered);
  }
}
