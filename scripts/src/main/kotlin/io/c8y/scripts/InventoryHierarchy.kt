package io.c8y.scripts

import io.c8y.api.inventory.*
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class ManagedObjectNode(
    val id: String,
    val name: String
)

class InventoryHierarchy(private val inventoryApi: InventoryApi) {
    private val store: ConcurrentMap<String, ManagedObjectNode> = ConcurrentHashMap()
    private val byName: ConcurrentMap<String, String> = ConcurrentHashMap()
    private val parents: ConcurrentMap<String, MutableSet<String>> = ConcurrentHashMap()
    private val children: ConcurrentMap<String, MutableSet<String>> = ConcurrentHashMap()

    fun store(mo: ManagedObject): ManagedObjectNode {
        val id = mo.id!!
        if (id in store) {
            return store[id]!!
        }
        val name = mo.name
        val node = ManagedObjectNode(id, name ?: "")
        store[id] = node
        if (!name.isNullOrBlank()) {
            val existing = byName[name]
            require(existing == null || existing == id) { "Duplicated name $name : ${id} with $existing" }
            byName[name] = mo.id
        }
        return node

    }

    fun addChild(parent: String, child: String) {
        this.addChildren(parent, setOf(child))
    }

    fun addChildren(parent: String, childAssets: Collection<String>) {
        childAssets.forEach { child ->
            parents.getOrPut(child, { ConcurrentHashMap.newKeySet() })
                .add(parent)
        }
        children.getOrPut(parent, { ConcurrentHashMap.newKeySet() }).addAll(childAssets)
    }

    fun findByName(name: String): Mono<ManagedObject> {
        return findIdByName(name).flatMap {
            inventoryApi.get(it)
        }
    }

    fun findIdByName(name: String): Mono<String> {
        return Mono.fromCallable { byName[name] }
    }

    fun load(): Mono<InventoryHierarchy> {
        return inventoryApi.list("pageSize" to "2000", "skipChildrenNames" to false)
            .map { mo ->
                store(mo)
                mo
            }.buffer(Runtime.getRuntime().availableProcessors() * 2)
            .flatMap { mos ->
                Flux.fromIterable(mos)
                    .map { mo ->
                        addChildren(mo.id!!, mo.childAssets?.mapNotNull { it.managedObject.id!! } ?: setOf<String>())
                    }
            }
            .then()
            .mapNotNull { this }
            .switchIfEmpty(Mono.just(this))


    }

    fun deepFor(id: String): Int {
        var deep = 0
        var current = id;
        while (true) {
            if (current in parents) {
                current = parents[current]?.first()!!
                deep++
            } else {
                break
            }
        }
        return deep
    }

    fun ensureSubgroup(parent: String, name: String): Mono<ManagedObject> {
        return findIdByName(name).switchIfEmpty {
            inventoryApi.ensureSubGroup(name = name)
                .doOnNext {
                    store(it)
                }.map { it.id!! }
        }.flatMap { child ->
            ensureIsChild(parent, child)
                .map { child }.switchIfEmpty(Mono.just(child))
        }
            .flatMap { inventoryApi.get(it) }


    }

    fun ensureIsChild(parent: String, child: String): Mono<Void> {
        return if (child in childrenOf(parent)) {
            Mono.empty()
        } else {
            inventoryApi.addChildAsset(parent, child).doOnSuccess {
                addChild(parent, child)
            }
        }

    }

    fun countChildren(id: String): Int {
        return childrenOf(id).size
    }


    private fun childrenOf(id: String) = children[id] ?: setOf()

    fun ensureDevice(parent: String, name: String): Mono<ManagedObject> {
        return findIdByName(name).switchIfEmpty {
            inventoryApi.ensureDevice(name = name)
                .doOnNext {
                    store(it)
                }.map { it.id!! }
        }.flatMap { child ->
            ensureIsChild(parent, child)
                .map { child }.switchIfEmpty(Mono.just(child))
        }
            .flatMap { inventoryApi.get(it) }
    }
}

fun main() {
    val api = Platform["local"]
    Flux.range(0, 50)
        .subscribeOn(Schedulers.newBoundedElastic(10, 10, "test"))
        .subscribe {
            api.rest().tenant().ensureTenant("jaro-$it")
                .map { tenant ->
                    api.forTenant(tenant)
                }.subscribe { rest ->
                    val inventory = rest.rest().inventory()
                    InventoryHierarchy(inventory).load()
                        .subscribe { hierarchy ->

                            val subGroups = 2
                            val hierarchyDeep = 3
                            val devicesPerGroup = 1000

                            Flux.range(0, subGroups)
                                .concatMap { index ->
                                    val name = "${index.toString().reversed()} Top level group $index"
                                    hierarchy.findByName(name)
                                        .switchIfEmpty(
                                            inventory.ensureGroup(name)
                                        )
                                }
                                .flatMap { parents ->
                                    onNextItem(hierarchy, parents, hierarchyDeep, subGroups, devicesPerGroup)
                                }.subscribe()
                        }

                }
        }
    Thread.sleep(100000000)

}

private fun onNextItem(
    hierarchy: InventoryHierarchy,
    mo: ManagedObject,
    hierarchyDeep: Int,
    subGroups: Int,
    devicesPerGroup: Int
): Flux<ManagedObject> {
    return Mono.just(mo)
        .flatMapMany { parent ->
            if (hierarchy.deepFor(parent.id!!) < hierarchyDeep) {
                Flux.range(0, subGroups)
                    .concatMap {
                        hierarchy.ensureSubgroup(
                            parent.id!!,
                            "${parent.name} Sub group $it"
                        )
                    }.flatMap {
                        onNextItem(hierarchy, it, hierarchyDeep, subGroups, devicesPerGroup)
                    }
            } else {
                Flux.range(0, devicesPerGroup)
                    .concatMap {
                        hierarchy.ensureDevice(parent.id!!, "${parent.name} Device $it")
                    }

            }
        }
}