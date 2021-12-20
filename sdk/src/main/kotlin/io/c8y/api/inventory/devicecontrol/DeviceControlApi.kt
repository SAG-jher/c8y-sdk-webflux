package io.c8y.api.inventory.devicecontrol

import com.fasterxml.jackson.annotation.JsonFormat
import io.c8y.api.inventory.ManagedObject
import io.c8y.api.support.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZonedDateTime


data class Operation(
    val id: String? = null,
    val deviceId: String? = null,
    val agentId: String? = null
) : Dynamic<Operation>()

data class OperationCollection(val operations: Iterable<Operation>, override val statistics: Page? = null) :
    Pageable<Operation> {
    override fun iterator(): Iterator<Operation> {
        return operations.iterator()
    }
}

data class BulkOperation(
    val id:Long? = null,
    val groupId: String ,
    @JsonFormat(shape = JsonFormat.Shape.STRING, with = [JsonFormat.Feature.WRITE_DATES_WITH_ZONE_ID], locale = "en_GB")
    val startDate: OffsetDateTime = ZonedDateTime.now().toOffsetDateTime(),
    val creationRamp: Long ,
    val operationPrototype: Map<String,Any>
)


class DeviceControlApi(private val client: WebClient) {
    fun create(mo: Operation): Mono<Operation> {
        return client.post()
            .uri { uri -> uri.path("devicecontrol/operations").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(mo), Operation::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Operation::class.java)
    }


    fun list(vararg params: Pair<String, Any>): Flux<Operation> {
        return Paging(client, path = "devicecontrol/operations")
            .list<Operation, OperationCollection>(params)
    }

    fun bulk():BulkOperationApi{
        return BulkOperationApi(client)
    }

    fun update(id: String, vararg update:Pair<String,Any?>): Mono<Operation> {
        return client.put()
            .uri { uri -> uri.path("devicecontrol/operations/$id").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(mapOf(*update)), Map::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Operation::class.java)
    }


}

class BulkOperationApi(private val client: WebClient){
    fun create(op:BulkOperation):Mono<BulkOperation>{
        return client.post()
            .uri { uri -> uri.path("devicecontrol/bulkoperations").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(op), BulkOperation::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(BulkOperation::class.java)
    }
}
