package io.c8y.api.management.tenant

import io.c8y.api.PlatformApi
import io.c8y.api.support.loggerFor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun TenantApi.tenants(numberOfTenants: Int, create: Boolean = false, batchSize: Int = 5): Flux<Tenant> {
    return Flux.range(0, numberOfTenants)
        .map { "jaro-$it" }
        .buffer(batchSize)

        .concatMap { ids ->
            Flux.fromIterable(ids).flatMap { id ->
                get(id).onErrorResume {
                    if (create) create(
                        Tenant(
                            id = id
                        )
                    ).onErrorResume { Mono.empty() } else Mono.empty()
                }
            }
        }
}
private val log = loggerFor<TenantApi>()
fun TenantApi.ensureTenant(id: String): Mono<Tenant> {
    return get(id).onErrorResume {
        create(
            Tenant(
                id = id
            )
        ).onErrorResume {
            log.warn("Failed to create tenant",it)
            get(id)
        }
    }
}

fun Tenant.from(api: PlatformApi): PlatformApi {
    return api.forTenant(this)
}

