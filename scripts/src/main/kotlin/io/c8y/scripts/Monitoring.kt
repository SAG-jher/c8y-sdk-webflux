package io.c8y.scripts

import com.fasterxml.jackson.databind.ObjectMapper
import io.c8y.api.inventory.event.Event
import io.c8y.config.Platform
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.OffsetDateTime


fun main() {


    val mapper = Jackson2ObjectMapperBuilder.json()
        .build<ObjectMapper>()
    val list = Platform["staging-monitoring"].rest().event().list(
        "source" to "1115114701",
        "type" to "monitoring_top_ram_processes",
        "dateFrom" to "2020-02-06",
        "pageSize" to "500"
    )
        .collect({
            mutableListOf<Pair<OffsetDateTime, Int>>()
        }, { aggregate, event ->
            fun resolve(event: Event): Int {
                val list: List<Map<String, Any>> =
                    mapper.readValue(event.text, List::class.java) as List<Map<String, Any>>
                val karaf = list.first {
                    it["username"] == "karaf"
                }
                return karaf["mem_size"] as Int
            }

            when {
                aggregate.isEmpty() -> {
                    aggregate.add(event.time to resolve(event))
                }
                aggregate.last().first.minusHours(1).isAfter(event.time) -> {
                    val last = aggregate.last().second
                    aggregate.add( event.time to  resolve(event))
                }
                else -> {}
            }
        })
        .block()
    list.reverse()
    list.map {
        it.first to "${(it.second / 1024.0 / 1024.0)}GB"
    }.forEach {
        println(it)
    }
}
