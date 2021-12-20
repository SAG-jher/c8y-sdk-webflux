package io.c8y.scripts

import com.google.common.base.Stopwatch
import io.c8y.api.inventory.ensureDevice
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.math.RandomUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


fun main() {
    val api = Platform["staging-latest"]
    val rest = api.rest().tenant().ensureTenant("jaro-0").map { tenant ->
        api.forTenant(tenant)
    }.block()
    val inventory = rest.rest().inventory()

    val createStarted = Stopwatch.createStarted()
    Flux.range(0, 1000)
        .flatMap {
            inventory.ensureDevice(
                name = "large_mo_v2_${it}",
                fragments = (0..1000).map { prop ->
                    "property $prop" to randomPropertyValue()
                }.toTypedArray()
            )
        }.flatMap { mo->
            inventory.update(
                mo.id!!,
                (0..2).map{RandomUtils.nextInt(2500)}.map { prop ->
                    "property $prop" to randomPropertyValue()
                }.toMap()
            ).onErrorResume {
                println(it.message)
                    Mono.just(mo)
                }
        }
        .doOnNext { println(it.id) }
        .blockLast()
    println("took ${createStarted.stop()}")
}

private fun randomPropertyValue() = (0..100).map {
    RandomStringUtils.randomAlphabetic(21)
}.joinToString(" ") { it }