package io.c8y.scripts

import io.c8y.api.inventory.measurement.Measurement
import io.c8y.api.inventory.measurement.MeasurementCollection
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import io.c8y.scripts.support.log
import org.apache.commons.lang.RandomStringUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun main() {
    val current = Platform["staging-latest"]
    val api = current.rest().tenant()
        .ensureTenant("jaro-0")
        .map { current.forTenant(it) }
        .block()

    val valuesPerFragment = 1

    api.rest().inventory().list()
        .concatMap { device ->
            log.info("count for {}",device.id)
            api.rest().measurement().list("source" to device.id!!).count()
                .flatMap { count ->
                    log.info("count done for {} items {}",device.id,count)
                    if (count > 1000) {
                        Mono.just(device)
                    } else {

                        Mono.defer {
                            Flux.range(0,1000).map {
                                val temp = (0..valuesPerFragment).map {
                                    "T" + it to mapOf("value" to it, "unit" to RandomStringUtils.randomAlphabetic(1000))
                                }.toMap()
                                Measurement(source = device, type = "measurement").set(
                                    "c8y_Temperature", temp
                                )
                            }.collectList()
                                .map {
                                    MeasurementCollection(measurements =  it)
                                }
                                .flatMap {
                                    api.rest().measurement().createMany(it)
                                }
                                .map { device }
                        }
                    }
                }

        }
        .buffer(1000)
        .flatMapIterable { it }
        .flatMap {
            log.info("get for {}",it.id)
            api.rest().measurement().list("source" to it.id!!)
        }
        .buffer(2000)
        .doOnNext {
            log.info("${it.size} measurements read")
        }
        .blockLast()

}





