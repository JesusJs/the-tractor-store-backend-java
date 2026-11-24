/** Catalog module. Other modules may only use its {@code api} package or its events. */
@ApplicationModule(
    displayName = "Catalog",
    allowedDependencies = {"shared"})
package com.tractorstore.catalog;

import org.springframework.modulith.ApplicationModule;
