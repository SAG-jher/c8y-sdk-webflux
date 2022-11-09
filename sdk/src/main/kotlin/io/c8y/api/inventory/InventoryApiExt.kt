package io.c8y.api.inventory

import reactor.core.publisher.Mono

fun InventoryApi.ensureExistsByType(
    type: String,
    name: String,
    configure: ManagedObject.() -> ManagedObject
): Mono<ManagedObject> {
    return list("query" to "\$filter=type eq '$type' and name eq '$name'", "pageSize" to "1")
        .take(1)
        .doOnNext { device ->
            log.info("$type found $device")
        }
        .singleOrEmpty()
        .switchIfEmpty(

            create(
                ManagedObject(
                    type = type,
                    name = name,
                ).configure()
            ).doOnNext { device ->
                log.info("$type created $device")
            }
        )
}


fun InventoryApi.listGroups(vararg params: Pair<String, Any>) =
    this.list("fragmentType" to "c8y_IsDeviceGroup", "withChildren" to "false", *params)

fun InventoryApi.listDevices(vararg params: Pair<String, Any>) =
    this.list("fragmentType" to "c8y_IsDevice", "withChildren" to "false", *params)

fun InventoryApi.ensureGroup(name: String = "c8y_test"): Mono<ManagedObject> {
    return ensureExistsByType("c8y_DeviceGroup", name) {
        set("c8y_IsDeviceGroup", emptyMap<Any, Any>())
    }
}


fun InventoryApi.ensureSubGroup(name: String = "c8y_test"): Mono<ManagedObject> {
    return ensureExistsByType("c8y_DeviceSubgroup", name) {
        set("c8y_IsDeviceGroup", emptyMap<Any, Any>())
    }

}

fun InventoryApi.ensureChildAsset(parentId: String, childId: String): Mono<Boolean> {
    return listChildAssets(parentId)
        .any {
            it.id == childId
        }
        .flatMap { isChild ->
            if (!isChild) {
                addChildAsset(parentId, childId)
                    .map { true }
            } else {
                Mono.just(false)
            }
        }
}

fun InventoryApi.ensureDevice(
    type: String = "c8y_test",
    name: String = "Test Device",
    requiredInterval: Long = -1,
    vararg fragments: Pair<String, Any>
): Mono<ManagedObject> {
    return ensureExistsByType(type, name) {
        copy(availability = requiredInterval.let { if (requiredInterval >= 0) RequiredAvailability(responseInterval = requiredInterval) else null })
            .set("c8y_IsDevice", emptyMap<Any, Any>())
            .also { mo ->
                fragments.forEach { fragment ->
                    mo.set(fragment.first, fragment.second)
                }
            }


    }
}
