/** Inventory module. Other modules may only use its {@code api} package or its events. */
@ApplicationModule(displayName = "Inventory", allowedDependencies = "order :: api")
package com.tractorstore.inventory;

import org.springframework.modulith.ApplicationModule;
