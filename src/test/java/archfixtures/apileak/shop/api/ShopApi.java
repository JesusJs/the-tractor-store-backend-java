package archfixtures.apileak.shop.api;

import archfixtures.apileak.shop.domain.Thing;

/** Violation: the public api exposes a domain type. */
public interface ShopApi {

  Thing find(String id);
}
