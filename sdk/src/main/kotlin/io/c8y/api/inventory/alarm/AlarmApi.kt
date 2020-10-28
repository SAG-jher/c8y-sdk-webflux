package io.c8y.api.inventory.alarm

import com.fasterxml.jackson.annotation.JsonFormat
import io.c8y.api.inventory.ManagedObject
import io.c8y.api.support.Dynamic
import io.c8y.api.support.handleRestError
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class AlarmApi(val baseUrl: UriBuilderFactory, val client: WebClient) {

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

}

enum class Severity {
    MAJOR
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