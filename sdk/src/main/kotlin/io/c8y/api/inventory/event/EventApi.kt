package io.c8y.api.inventory.event

import com.fasterxml.jackson.annotation.JsonFormat
import io.c8y.api.inventory.ManagedObject
import io.c8y.api.support.Dynamic
import io.c8y.api.support.Page
import io.c8y.api.support.Pageable
import io.c8y.api.support.Paging
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZonedDateTime


data class EventCollection(val events: Iterable<Event>, override val statistics: Page? = null) :
    Pageable<Event> {
    override fun iterator(): Iterator<Event> {
        return events.iterator()
    }
}

data class Event(
    val id: String? = null,
    val source: ManagedObject,
    @JsonFormat(shape = JsonFormat.Shape.STRING, with = [JsonFormat.Feature.WRITE_DATES_WITH_ZONE_ID], locale = "en_GB")
    val time: OffsetDateTime = ZonedDateTime.now().toOffsetDateTime(),
    val type: String,
    val text: String
) : Dynamic<Event>()

class EventApi(private val client: WebClient) {
    fun create(mo: Mono<Event>): Mono<Event> {
        return client.post()
            .uri { uri -> uri.path("event/events").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mo, Event::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Event::class.java)
    }

    fun list(vararg params: Pair<String, Any>): Flux<Event> {
        return Paging(client, path = "event/events")
            .list<Event, EventCollection>(params)
    }
}



