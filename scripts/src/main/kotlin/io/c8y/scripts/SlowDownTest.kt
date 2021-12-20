package io.c8y.scripts

import com.google.common.base.Stopwatch
import io.c8y.api.inventory.ensureDevice
import io.c8y.api.inventory.measurement.Measurements
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.api.support.info
import io.c8y.api.support.loggerFor
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

fun main(args:Array<String>) {
    val log = loggerFor("SlowDownTest")
    val current = Platform[args[0]]
    val api = current.rest().tenant()
        .ensureTenant("jaro-0")
        .map { current.forTenant(it) }
        .block()
    val id = AtomicLong()
    val random = System.currentTimeMillis()
    val aggregate: Average = Average()
    log.info { "Starting" }
    Flux.generate<Long> { sink ->
        sink.next(id.incrementAndGet())
    }.buffer(1).concatMap {
        Flux.fromIterable(it).flatMap {

            val sla = Stopwatch.createStarted()
            Mono.just(it)
                .flatMap { currentId ->
                    api.rest().inventory().ensureDevice(
                        type = "c8y_stability_test_${random}_$currentId",
                        name = "Stability Test Device $currentId"
                    )
                }
                .flatMap { device ->
                    api.rest().measurement().create(Measurements.temperature(device, id.get()))
                        .map { device }
                }
                .flatMap { device ->
                    api.rest().measurement().list("source" to device.id!!)
                        .collectList()
                        .map { device }
                }
                .flatMap { device ->
                    api.rest().inventory().delete(device.id!!)
                }
                .doFinally {
                    aggregate.aggregate(sla.stop().elapsed(TimeUnit.MILLISECONDS))
                    log.info { "SLA ${aggregate}" }
                }
        }

    }
        .blockLast()

}


class Average {
    private var avg: BigDecimal = BigDecimal.valueOf(0L, 2);
    private var max: Long = Long.MIN_VALUE
    private var min: Long = Long.MAX_VALUE
    private val iter = AtomicLong()

    private val change: BigDecimal = BigDecimal.valueOf(0.1)
    private val mc = MathContext(2, RoundingMode.CEILING)
    val value: BigDecimal get() = avg

    fun aggregate(newValue: Long) {
        synchronized(this) {
            val delta = (BigDecimal.valueOf(newValue).minus(avg)).multiply(change, mc)
            avg = avg.add(delta)
            max = Math.max(max, newValue)
            min = Math.min(min, newValue)
            if (iter.incrementAndGet() % 500 == 0L) {
                reset(newValue)
            }
        }
    }

    private fun reset(newValue: Long) {
        min = newValue
        max = newValue
    }

    override fun toString(): String {
        return "avg=${avg.toPlainString()}, max=${max}, min=${min}"
    }
}




