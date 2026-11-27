package archfixtures.domainapp.shop.domain;

import archfixtures.domainapp.shop.application.ShopService;

/** Violation: domain depends on application. */
public class ApplicationAwareDomain {

  ShopService service;
}
