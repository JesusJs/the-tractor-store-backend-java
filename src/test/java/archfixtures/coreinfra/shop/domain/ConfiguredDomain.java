package archfixtures.coreinfra.shop.domain;

import archfixtures.coreinfra.shop.infrastructure.config.ShopSettings;

/** Violation: domain depends on infrastructure. */
public class ConfiguredDomain {

  ShopSettings settings;
}
