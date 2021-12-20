package io.c8y.scripts

import io.c8y.api.inventory.listDevices
import io.c8y.api.inventory.listGroups
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.api.support.info
import io.c8y.api.support.loggerFor
import io.c8y.config.Platform
import reactor.core.publisher.Mono

fun main() {
    val log = loggerFor("Cleanup")
    val api = Platform["local"]
    val rest = api.rest().tenant().ensureTenant("jaro-0").map { tenant ->
        api.forTenant(tenant)
    }.block()!!
    val inventory = rest.rest().inventory()

    inventory.listGroups()
        .concatWith(inventory.listDevices())
        .map { it.id }
        .filter { it != null }
        .map { it!! }
        .distinct()
        .collectList()
        .doOnNext {
            log.info {
                "Managed objects to cleanup ${it.size}"
            }
        }
        .flatMapIterable { it }
        .concatMap {
            inventory.delete(it).onErrorResume { Mono.empty<Void>() }
        }
        .collectList()
        .block()
}