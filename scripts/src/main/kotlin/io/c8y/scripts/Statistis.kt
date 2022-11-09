package io.c8y.scripts

import com.fasterxml.jackson.databind.ObjectMapper
import io.c8y.api.inventory.listDevices
import io.c8y.api.inventory.listGroups
import io.c8y.config.Platform
import io.c8y.scripts.support.Metrics
import io.c8y.scripts.support.RichGauge
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.OffsetDateTime


fun main() {
    val platform = Platform["schindler-prod"]
//    val devices = local.rest().inventory().listDevices().count().block()
//
//    val groups = local.rest().inventory().listGroups().count().block()
//    val largestGroups = local.rest().inventory().listGroups().flatMap { group ->
//        local.rest().inventory().listChildAssets(group.id!!)
//            .count()
//            .map { children ->
//                "${group.name}(${group.id})" to children
//            }
//    }.sort { first, second -> second.second.compareTo(first.second) }
//
//    .take(10)
//    .collectList()
//    .map {
//        it
//            .joinToString("\n") { "${it.first} => ${it.second}" }
//    }
//    .block()
//    println("Devices ${devices}")
//    println("Groups ${groups}")
//    println("Larges $largestGroups")
    val mapper = Jackson2ObjectMapperBuilder.json().build<ObjectMapper>()
    val metrics = mutableMapOf<String, RichGauge>()

    val rest = platform.rest()

    rest.inventory().listGroups("withChildren" to true)
        .map {
            it.childAssets?.references?.count() ?: 0
        }.subscribe {
            metrics.computeIfAbsent("groups.children.count", Metrics::gauge)
                .aggregate(it.toDouble())
        }

    val blockFirst = rest.inventory()
        .listDevices("pageSize" to 1000, "currentPage" to 20)

        .filter {
            it.lastUpdated?.isAfter(OffsetDateTime.now().minusWeeks(1)) ?: false
        }
        .buffer(5)
        .map { it.shuffled().first() }
        .take(5000)
        .doOnNext {
            val writeValueAsBytes = mapper.writeValueAsBytes(it)
            metrics.computeIfAbsent("device.managedObject.sizeInBytes", Metrics::gauge)
                .aggregate(writeValueAsBytes.size.toDouble())
            metrics.computeIfAbsent("device.managedObject.${it.type}.sizeInBytes", Metrics::gauge)
                .aggregate(writeValueAsBytes.size.toDouble())
        }
        .concatMap { device ->
            rest.event().count("source" to device.id!!, "dateFrom" to OffsetDateTime.now().minusMonths(1))
                .doOnNext {
                    it?.let { it.toDouble() }?.apply {
                        metrics.computeIfAbsent("device.event.per-device.count", Metrics::gauge)
                            .aggregate(this)
                        metrics.computeIfAbsent("device.event.per-device.${device.type}.count", Metrics::gauge)
                            .aggregate(this)
                    }
                }
                .then(
                    rest.measurement().list("source" to device.id!!, "dateFrom" to OffsetDateTime.now().minusMonths(1))

                        .doOnNext {
                            val series = it.get()?.entries?.fold(0) { sum, entry ->
                                when (entry.value) {
                                    is Map<*, *> -> sum + (entry.value as Map<*, *>).size
                                    else -> {
                                        sum
                                    }
                                }
                            }?.toDouble()
                            series?.apply {
                                metrics.computeIfAbsent("device.measurement.${it.type}.series", Metrics::gauge)
                                    .aggregate(this)
                                metrics.computeIfAbsent("device.measurement.series", Metrics::gauge).aggregate(this)
                            }
                        }.count()
                        .doOnNext {
                            metrics.computeIfAbsent("device.measurement.per-device.count", Metrics::gauge)
                                .aggregate(it.toDouble())
                            metrics.computeIfAbsent(
                                "device.measurement.per-device.${device.type}.count",
                                Metrics::gauge
                            ).aggregate(it.toDouble())
                        }
                )
                .then(
                    rest.alarm().count("source" to device.id!!, "dateFrom" to OffsetDateTime.now().minusMonths(1))

                        .doOnNext {
                            it?.let { it.toDouble() }?.apply {
                                metrics.computeIfAbsent("device.alarm.per-device.count", Metrics::gauge)
                                    .aggregate(this)
                                metrics.computeIfAbsent("device.alarm.per-device.${device.type}.count", Metrics::gauge)
                                    .aggregate(this)
                            }
                        }
                ).then(
                    rest.devicecontrol()
                        .count("deviceId" to device.id!!, "dateFrom" to OffsetDateTime.now().minusMonths(1))
                        .doOnNext {
                            it?.let { it.toDouble() }?.apply {
                                metrics.computeIfAbsent("device.operation.per-device.count", Metrics::gauge)
                                    .aggregate(this)
                                metrics.computeIfAbsent(
                                    "device.operation.per-device.${device.type}.count",
                                    Metrics::gauge
                                )
                                    .aggregate(this)
                            }
                        })
        }
        .blockLast()

    println("Finished !!!!!!!!!!!!!!")
    metrics.entries.sortedBy { it.key }.forEach {
        println(it.value)
    }


}