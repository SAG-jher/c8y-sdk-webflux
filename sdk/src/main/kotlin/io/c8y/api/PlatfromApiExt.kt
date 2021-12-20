package io.c8y.api

import io.c8y.api.management.tenant.ensureTenant
import reactor.core.publisher.Mono

fun PlatformApi.usingTenant(tenantId: String): Mono<PlatformApi> {
    return this.rest().tenant().ensureTenant(tenantId)
        .map {
            this.forTenant(it)
        }
        .map { it!! }

}
