/**
 * Shared technical module: error hierarchy, trace id, CORS and the anonymous {@code ts_session}
 * cookie session (ADR 0009, ADR 0011). No business logic, entities or tables; it depends on no
 * other module.
 */
@ApplicationModule(
    displayName = "Shared",
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = {})
package com.tractorstore.shared;

import org.springframework.modulith.ApplicationModule;
