/**
 * Shared technical module: error hierarchy, trace id, CORS (ADR 0009). No business logic, entities
 * or tables; it depends on no other module.
 */
@ApplicationModule(
    displayName = "Shared",
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = {})
package com.tractorstore.shared;

import org.springframework.modulith.ApplicationModule;
