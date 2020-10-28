package io.c8y.api.inventory.devicecontrol

import io.c8y.api.support.Dynamic
import io.c8y.api.support.Page
import io.c8y.api.support.Pageable
import io.c8y.api.support.Paging
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


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



class DeviceControlApi(private val client: WebClient) {
    fun create(mo: Mono<Operation>): Mono<Operation> {
        return client.post()
            .uri { uri -> uri.path("devicecontrol/operations").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mo, Operation::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Operation::class.java)
    }


    fun list(vararg params: Pair<String, Any>): Flux<Operation> {
        return Paging(client, path = "devicecontrol/operations")
            .list<Operation, OperationCollection>(params)
    }
}
