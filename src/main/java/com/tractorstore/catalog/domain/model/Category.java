package com.tractorstore.catalog.domain.model;

import static com.tractorstore.catalog.domain.model.DomainChecks.requireText;

/** Product category ({@code classic} or {@code autonomous}) with its home teaser. */
public record Category(String code, String brandLabel, String teaserTitle, String teaserImage) {

  public Category {
    requireText(code, "code");
    requireText(brandLabel, "brandLabel");
    requireText(teaserTitle, "teaserTitle");
    requireText(teaserImage, "teaserImage");
  }
}
