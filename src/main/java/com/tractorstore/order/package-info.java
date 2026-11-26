/** Order module. Other modules may only use its {@code api} package or its events. */
@ApplicationModule(
    displayName = "Order",
    allowedDependencies = {"catalog :: api", "inventory :: api", "shared"})
package com.tractorstore.order;

import org.springframework.modulith.ApplicationModule;
