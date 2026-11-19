/** Cart module. Other modules may only use its {@code api} package or its events. */
@ApplicationModule(
    displayName = "Cart",
    allowedDependencies = {"catalog :: api", "inventory :: api", "order :: api"})
package com.tractorstore.cart;

import org.springframework.modulith.ApplicationModule;
