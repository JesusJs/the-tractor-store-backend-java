/** Order module. Other modules may only use its {@code api} package or its events. */
@ApplicationModule(
    displayName = "Order",
    allowedDependencies = {"cart :: api", "catalog :: api"})
package com.tractorstore.order;

import org.springframework.modulith.ApplicationModule;
