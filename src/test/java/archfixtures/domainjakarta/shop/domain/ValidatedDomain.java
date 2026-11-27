package archfixtures.domainjakarta.shop.domain;

import jakarta.validation.constraints.NotNull;

/** Violation: domain depends on Jakarta Validation. */
public class ValidatedDomain {

  @NotNull String name;
}
