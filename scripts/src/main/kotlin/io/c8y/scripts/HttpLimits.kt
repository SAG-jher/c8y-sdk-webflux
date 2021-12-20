package io.c8y.scripts

import io.c8y.api.inventory.ensureDevice
import io.c8y.api.inventory.measurement.Measurement
import io.c8y.api.inventory.measurement.createTemperatureMeasurementBatch
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicLong

fun main() {
    val management = Platform["staging-latest"]

    val tenant = management.rest().tenant().ensureTenant("jaro-0").block()!!
    management.rest().tenant().update(tenant.id, "customProperties" to mapOf("limit.http.requests" to 10,"limit.http.queue" to 10000))
        .block()
    val api = management.forTenant(tenant)
    val tps = Tps()
    val device = api.rest().inventory().ensureDevice().block()!!.toReference();
    Flux.range(0,1000)
        .limitRate(100)
        .flatMap {
            api.rest().measurement().createTemperatureMeasurementBatch(device = device,numerOfMeasurements = 1)
                .doOnNext {
                    tps.increment()
                }
        }.doFinally {
            tps.stop()
        }
        .last()
        .block()

    println("rate per sec ${tps.rate()}");

}


internal class Tps {
    private val transactions = ConcurrentHashMap<Long, AtomicLong>()
    private val clock = Clock.systemUTC();
    private val started = now()
    private var ended: Long = -1;
    fun increment() {
        if (ended != -1L) {
            return
        }
        val now = now()
        if (!transactions.containsKey(now)) {
            transactions.putIfAbsent(now, AtomicLong())
        }
        transactions[now]!!.incrementAndGet()
    }


    fun stop(): Tps {
        ended = now()
        return this
    }


    fun rate(): Long {
        if (transactions.isEmpty()) {
            return 0L
        }
        return if (transactions.size == 1) {
            transactions.values.iterator().next().get()
        } else {
            val start = started
            val end = ended
            var sum =  transactions.values.sumOf { it.get() }
            sum / Math.max(MILLISECONDS.toSeconds(end - start), 1)
        }
    }

    private fun now(): Long {
        return clock.millis()
    }

}
