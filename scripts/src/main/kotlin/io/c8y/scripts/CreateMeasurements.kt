package io.c8y.scripts

import io.c8y.api.inventory.ensureDevice
import io.c8y.api.inventory.measurement.createTemperatureMeasurementBatch
import io.c8y.api.management.realtime.RealtimeWebsocket
import io.c8y.api.management.tenant.tenants
import io.c8y.config.Platform
import io.c8y.scripts.support.log
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

fun main() {
    val current = Platform["staging-latest"]
    val elastic = Schedulers.boundedElastic()
    current.rest().tenant().tenants(1, true)
//        .concatMap { current.rest().tenant().delete(it.id!!) }
//        .subscribe()
        .map { current.forTenant(it) }
        .flatMap { api ->
            val rest = api.rest()
            Flux.range(0, 5000)
                .flatMap { device ->
                    rest.inventory()
                        .ensureDevice(type = "c8y_realtime_test_$device", requiredInterval = 30)
                }.subscribe { device ->
                    val tenant = api.tenant.cache()
                    elastic.createWorker().schedulePeriodically({
                        if (ThreadLocalRandom.current().nextBoolean()) {
                            tenant.subscribe { tenant ->
                                log.info("Skiping measurement creation for tenant {} and {}", tenant, device.id)
                            }
                        } else {
                            api.withBaseUrl(
                                "http://core-${
                                    ThreadLocalRandom.current().nextLong(2)
                                }.default.svc.cluster.local:8181/"
                            )
                                .rest().measurement().createTemperatureMeasurementBatch(device.toReference(), 1)
                                .subscribe {
                                    tenant.subscribe { tenant ->
                                        log.info("measurement created for tenant {} and {}", tenant, device.id)
                                    }
                                }
                        }
                    }, 20, 20, TimeUnit.MINUTES)
                }

            val realtime = RealtimeWebsocket(api, "cep/realtime")
            realtime
                .handshake().flatMapMany {
                    realtime.subscirbe("/alarms/*")
                }
        }


        .subscribe {
            println(it)
        }

    Thread.sleep(100000000)
}





