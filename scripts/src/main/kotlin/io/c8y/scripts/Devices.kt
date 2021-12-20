package io.c8y.scripts

import io.c8y.api.inventory.ManagedObject
import io.c8y.api.inventory.ManagedObjectRef
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun main() {
    val rest = Platform["staging-latest"].rest()


    val inventoryApi = rest.inventory()
    inventoryApi.create(
            ManagedObject(type = "c8y_test_device", name = "Test device 2")
                .set("c8y_IsDevice", mapOf<Any, Any>())
    )

        .flatMapMany { parent ->
            Flux.range(0, 6000)
                .concatMap { index ->
                    inventoryApi.create(ManagedObject(name = "Child asset $index"))
                }
                .concatMap { child ->
                    inventoryApi.addChildAsset(
                        parent = parent.id!!,
                        child = ManagedObjectRef(managedObject = child.toReference())
                    )
                }
        }
        .blockLast()


}


