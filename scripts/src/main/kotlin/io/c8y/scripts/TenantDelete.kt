package io.c8y.scripts

import io.c8y.api.*
import io.c8y.api.inventory.ensureDevice
import io.c8y.api.inventory.measurement.createTemperatureMeasurementBatch
import io.c8y.api.management.tenant.tenants
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.extra.retry.retryExponentialBackoff
import java.time.Duration

fun main() {


    val platform = Platform["local"]


    platform.rest().tenant().list()
        .filter {it.id != "management" }
//        .doOnNext { tenant ->
//            Mono.just(platform.forTenant(tenant))
//                .delayUntil {
//                    it.rest().tenant().currentTenant().get().retryExponentialBackoff(30,Duration.ofSeconds(20),)
//                }
//                .flatMapMany { rest ->
//                    Flux.range(0, 5000).map {
//                        "${tenant.id} - Device $it"
//                    }.concatMap { deviceName ->
//                        rest.rest().inventory().ensureDevice(deviceName)
//                    }.concatMap { device ->
//                        rest.rest().measurement().createTemperatureMeasurementBatch(device, 5000)
//                    }
//                }.subscribe()
//        }
        .flatMap { tenant ->
            platform.rest().tenant().delete(tenant.id!!).map { tenant }.onErrorContinue{ e,k->tenant}
        }
        .doOnEach {
            println(it)
        }
        .blockLast()

}


