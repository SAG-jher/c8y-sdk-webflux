package io.c8y.scripts

import io.c8y.api.inventory.alarm.Alarm
import io.c8y.api.inventory.alarm.Severity
import io.c8y.api.inventory.alarm.Status
import io.c8y.api.inventory.ensureDevice
import io.c8y.api.inventory.measurement.createTemperatureMeasurementBatch
import io.c8y.api.management.tenant.tenants
import io.c8y.config.Platform
import io.c8y.scripts.support.log
import reactor.core.publisher.Flux

fun main() {
    val current = Platform["staging-latest"]
    current.rest().tenant().tenants(1, true)
        .map { current.forTenant(it) }
        .flatMap { api ->
            val rest = api.rest()

            Flux.range(0, 500)
                .flatMap { device ->
                    rest.inventory()
                        .ensureDevice(type = "c8y_realtime_test_$device")
                        .flatMapMany { device ->
                            Flux.range(0, 10)
                                .flatMap {
                                    rest.measurement()
                                        .createTemperatureMeasurementBatch(device.toReference(), 5000)
                                        .onErrorContinue({ a, b -> log.warn("Faied to create batch", a) })
                                }.flatMap {
                                    rest.alarm().create(
                                        Alarm(
                                            source = device.toReference(),
                                            severity = Severity.MAJOR,
                                            status = Status.ACTIVE,
                                            text = "Alarm for device $it",
                                            type = "Alarm_$it"
                                        )
                                    )
                                }
                        }
                }


        }
        .blockLast()
}





