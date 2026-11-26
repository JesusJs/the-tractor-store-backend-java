/** Cart module. Other modules may only use its {@code api} package or its events. */
@ApplicationModule(
    displayName = "Cart",
    allowedDependencies = {"shared"})
package com.tractorstore.cart;

import org.springframework.modulith.ApplicationModule;
