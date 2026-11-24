package archfixtures.webrepo.shop.infrastructure.web;

import archfixtures.webrepo.shop.infrastructure.persistence.ShopRepository;

/** Violation: web adapter uses a repository instead of a use case. */
public class ShopController {

  ShopRepository repository;
}
