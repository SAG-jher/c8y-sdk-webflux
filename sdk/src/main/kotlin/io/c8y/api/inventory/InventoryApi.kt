package io.c8y.api.inventory

import com.fasterxml.jackson.annotation.JsonProperty
import io.c8y.api.inventory.event.Event
import io.c8y.api.inventory.event.EventCollection
import io.c8y.api.support.*
import io.c8y.api.support.PlatformMediaType.MANAGED_OBJECT_REFERENCE_COLLECTION_TYPE
import org.springframework.http.MediaType
import org.springframework.http.MediaType.parseMediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

data class RequiredAvailability(
    val responseInterval: Long = -1
)

data class ManagedObject(
    val id: String? = null,
    val type: String? = null,
    val name: String? = null,
    val self: String? = null,
    val lastUpdated: OffsetDateTime? = null,
    val childAssets: ManagedObjectRefCollection? = null,
    @JsonProperty("c8y_RequiredAvailability")
    val availability: RequiredAvailability? = null

) : Dynamic<ManagedObject>() {
    val isDevice: Boolean
        get() {
            return this.get()?.containsKey("c8y_IsDevice") == true
        }

    fun toReference(): ManagedObject {
        return ManagedObject(id = id!!)
    }
}

data class ManagedObjectRefCollection(
    val references: Iterable<ManagedObjectRef>,
    override val statistics: Page = Page.empty
) :
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
    fun create(mo: ManagedObject): Mono<ManagedObject> {
        return client.post()
            .uri { uri -> uri.path("inventory/managedObjects").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(mo), ManagedObject::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(ManagedObject::class.java)
    }

    fun update(id: String, vararg update: Pair<String, Any?>): Mono<ManagedObject> {
        return client.put()
            .uri { uri -> uri.path("inventory/managedObjects/$id").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(mapOf(*update)), Map::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(ManagedObject::class.java)
    }

    fun get(id: String, vararg params: Pair<String, Any>): Mono<ManagedObject> {
        return client.get()
            .uri { uri ->
                uri.path("inventory/managedObjects/$id").apply {
                    params.forEach {
                        queryParam(it.first, it.second)
                    }
                }.build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(ManagedObject::class.java)
    }

    fun update(id: String, update: Map<String, Any?>): Mono<ManagedObject> {
        return client.put()
            .uri { uri -> uri.path("inventory/managedObjects/$id").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(update), Map::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(ManagedObject::class.java)
    }

    fun addChildAsset(parent: String, child: String): Mono<Void> {
        return addChildAsset(parent, ManagedObjectRef(ManagedObject(id = child)))
    }

    fun addChildAsset(parent: String, child: ManagedObjectRef): Mono<Void> {
        return client.post()
            .uri { uri -> uri.path("inventory/managedObjects/${parent}/childAssets").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(child), ManagedObjectRef::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono()
    }

    fun addChildAsset(parent: String, child: ManagedObjectRefCollection): Mono<Void> {
        return client.post()
            .uri { uri -> uri.path("inventory/managedObjects/${parent}/childAssets").build() }
            .contentType(MANAGED_OBJECT_REFERENCE_COLLECTION_TYPE)
            .body(Mono.just(child), ManagedObjectRefCollection::class.java)
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
    fun count(vararg params: Pair<String, Any>): Mono<Long?> {
        return Paging(client, path = "inventory/managedObjects")
            .count<ManagedObject, ManagedObjectCollection>(params)
    }

    fun listChildAssets(id: String, vararg params: Pair<String, Any>): Flux<ManagedObject> {
        return Paging(client, path = "inventory/managedObjects/${id}/childAssets")
            .list<ManagedObjectRef, ManagedObjectRefCollection>(params)
            .map { it.managedObject }
    }

    fun delete(id: String): Mono<Void> {
        return client.delete()
            .uri { uri -> uri.path("inventory/managedObjects/${id}").build() }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono()

    }
}
