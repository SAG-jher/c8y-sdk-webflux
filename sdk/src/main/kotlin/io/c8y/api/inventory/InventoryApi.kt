package io.c8y.api.inventory

import com.fasterxml.jackson.annotation.JsonProperty
import io.c8y.api.support.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class RequiredAvailability(
    val responseInterval: Int = -1
)

data class ManagedObject(
    val id: String? = null,
    val type: String? = null,
    val name: String? = null,
    @JsonProperty("c8y_RequiredAvailability")
    val availability: RequiredAvailability? = null
) : Dynamic<ManagedObject>() {
    fun toReference(): ManagedObject {
        return ManagedObject(id = id!!)
    }
}

data class ManagedObjectRefCollection(val references: Iterable<ManagedObjectRef>, override val statistics: Page) :
    Pageable<ManagedObjectRef> {
    override fun iterator(): Iterator<ManagedObjectRef> {
        return references.iterator()
    }

}

data class ManagedObjectRef(val managedObject: ManagedObject)


data class ManagedObjectCollection(val managedObjects: Iterable<ManagedObject>, override val statistics: Page) :
    Pageable<ManagedObject> {
    override fun iterator(): Iterator<ManagedObject> {
        return managedObjects.iterator()
    }
}

class InventoryApi(private val client: WebClient) {
    val log = loggerFor<InventoryApi>()
    fun create(mo: Mono<ManagedObject>): Mono<ManagedObject> {
        return client.post()
            .uri { uri -> uri.path("inventory/managedObjects").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(mo, ManagedObject::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(ManagedObject::class.java)
    }

    fun addChildAsset(parent: String, child: String): Mono<Void> {
        return addChildAsset(parent, Mono.just(ManagedObjectRef(ManagedObject(id = child))))
    }

    fun addChildAsset(parent: String, child: Mono<ManagedObjectRef>): Mono<Void> {
        return client.post()
            .uri { uri -> uri.path("inventory/managedObjects/${parent}/childAssets").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(child, ManagedObjectRef::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono()
    }

    fun list(vararg params: Pair<String, Any>): Flux<ManagedObject> {
        return Paging(client, path = "inventory/managedObjects")
            .list<ManagedObject, ManagedObjectCollection>(params.let { queryParams ->
                if (!queryParams.any { it.first == "skipChildrenNames" }) {
                    queryParams
                        .toMutableList()
                        .plusElement("skipChildrenNames" to "true")
                        .toTypedArray()
                } else {
                    queryParams
                }
            })
    }

    fun listChildAssets(id: String, vararg params: Pair<String, Any>): Flux<ManagedObject> {
        return Paging(client, path = "inventory/managedObjects/${id}/childAssets")
            .list<ManagedObjectRef, ManagedObjectRefCollection>(params)
            .map { it.managedObject }
    }
}
