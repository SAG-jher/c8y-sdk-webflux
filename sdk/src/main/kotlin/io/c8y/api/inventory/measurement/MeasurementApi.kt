package io.c8y.api.inventory.measurement

import com.fasterxml.jackson.annotation.JsonFormat
import io.c8y.api.inventory.ManagedObject
import io.c8y.api.support.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class MeasurementApi(private val client: WebClient) {
    internal val log = loggerFor<MeasurementApi>()
    fun create(mo: Measurement): Mono<Measurement> {
        return client.post()
            .uri { uri -> uri.path("measurement/measurements").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(mo), Measurement::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Measurement::class.java)
    }

    fun createMany(mo: MeasurementCollection): Mono<MeasurementCollection> {
        return client.post()
            .uri { uri -> uri.path("measurement/measurements").build() }
            .contentType(MediaType.parseMediaType("application/vnd.com.nsn.cumulocity.measurementCollection+json"))
            .body(Mono.just(mo), MeasurementCollection::class.java)
            .accept(MediaType.parseMediaType("application/vnd.com.nsn.cumulocity.measurementCollection+json"))
            .retrieve()
            .handleRestError()
            .bodyToMono(MeasurementCollection::class.java)
    }

    fun list(vararg params: Pair<String, Any>): Flux<Measurement> {
        return Paging(client, path = "measurement/measurements")
            .list<Measurement, MeasurementCollection>(params)

    }
}

data class MeasurementCollection(val measurements: Iterable<Measurement>, override val statistics: Page? = null) :
    Pageable<Measurement> {
    override fun iterator(): Iterator<Measurement> {
        return measurements.iterator()
    }
}

data class Measurement(
    val id: String? = null,
    val source: ManagedObject,
    @JsonFormat(shape = JsonFormat.Shape.STRING, with = [JsonFormat.Feature.WRITE_DATES_WITH_ZONE_ID], locale = "en_GB")
    val time: OffsetDateTime = ZonedDateTime.now().toOffsetDateTime(),
    val type: String
) : Dynamic<Measurement>()

object Measurements{
    fun temperature(device: ManagedObject, value: Number): Measurement {
        return Measurement(source = device.toReference(), type = "measurement").set(
            "c8y_Temperature",
            mapOf("T" to mapOf("value" to value, "unit" to "C"))
        )
    }
}

data class MeasurementValue(
    val value: Any,
    val unit: String? = null
)


fun MeasurementApi.createTemperatureMeasurementBatch(
    device: ManagedObject,
    numerOfMeasurements: Int
): Mono<MeasurementCollection> {
    log.info("Create batch of $numerOfMeasurements temparture measurements for device $device ")
    return createMany(
        MeasurementCollection(
            measurements = (1..numerOfMeasurements).map { value ->
                Measurement(source = device, type = "measurement").set(
                    "c8y_Temperature",
                    mapOf("T" to mapOf("value" to value, "unit" to "C"))
                )
            }
        )
    )
}