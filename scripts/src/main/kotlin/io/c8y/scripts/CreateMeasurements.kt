package io.c8y.scripts

import io.c8y.api.inventory.ensureDevice
import io.c8y.api.inventory.measurement.Measurement
import io.c8y.api.inventory.measurement.MeasurementCollection
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import io.c8y.scripts.support.log
import org.apache.commons.lang3.RandomStringUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

fun main() {
    val current = Platform["local"]
    val api = current.rest().tenant()
        .ensureTenant("jaro-0")
        .map { current.forTenant(it) }
        .block()

    Flux.range(0,10)
        .concatMap {
            api.rest().inventory().ensureDevice("c8y_measurement_test_$it")
        }
        .blockLast()

    val valuesPerFragment = 10

    api.rest().inventory().list()
        .concatMap { device ->
            log.info("count for {}",device.id)
            api.rest().measurement().list("source" to device.id!!).count()
                .flatMap { count ->
                    log.info("count done for {} items {}",device.id,count)
                    if (count > 100000) {
                        Mono.just(device)
                    } else {

                        Mono.defer {
                            Flux.range(0,5000).map {measurement->
                                val temp = (0..valuesPerFragment).map {
                                    "T" + it to mapOf("value" to it, "unit" to RandomStringUtils.randomAlphabetic(1000))
                                }.toMap()
                                Measurement(source = device, type = "measurement",time = OffsetDateTime.now().minusMonths(1).minusMinutes(measurement.toLong())).set(
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

        .blockLast()

}





