package io.c8y.api.inventory.alarm

import com.fasterxml.jackson.annotation.JsonFormat
import io.c8y.api.inventory.ManagedObject
import io.c8y.api.inventory.ManagedObjectCollection
import io.c8y.api.support.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class AlarmApi(val client: WebClient) {

    fun create(alarm: Alarm): Mono<Alarm> {
        return client.post()
            .uri { uri -> uri.path("alarm/alarms").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(alarm), Alarm::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Alarm::class.java)
    }

    fun update(alarm: Alarm):Mono<Alarm> {
        return client.put()
            .uri { uri -> uri.path("alarm/alarms/${alarm.id!!}").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(alarm), Alarm::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Alarm::class.java)
    }
    fun update(id:String,vararg update: Pair<String,Any?>): Mono<Alarm> {
        return client.put()
            .uri { uri -> uri.path("alarm/alarms/$id").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(mapOf(*update)), Map::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Alarm::class.java)
    }
    fun list(vararg params: Pair<String, Any>): Flux<Alarm> {
        return Paging(client, path = "alarm/alarms")
            .list<Alarm, AlarmCollection>(params)
    }

}

enum class Severity {
    MAJOR,MINOR,CRITICAL
}

enum class Status {
    ACTIVE, CLEARED
}

data class Alarm(
    val id: String? = null,
    val source: ManagedObject,
    val text: String,
    val severity: Severity,
    val status: Status,
    val type: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, with = [JsonFormat.Feature.WRITE_DATES_WITH_ZONE_ID], locale = "en_GB")
    val time: OffsetDateTime = ZonedDateTime.now().toOffsetDateTime()
) : Dynamic<Alarm>()

data class AlarmCollection(val alarms: Iterable<Alarm>, override val statistics: Page) :
    Pageable<Alarm> {
    override fun iterator(): Iterator<Alarm> {
        return alarms.iterator()
    }
}
