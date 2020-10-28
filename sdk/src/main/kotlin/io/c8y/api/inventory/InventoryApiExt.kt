package io.c8y.api.inventory

import com.cumulocity.model.Agent
import reactor.core.publisher.Mono


fun InventoryApi.ensureDevice(type: String = "c8y_test", name: String = "Test Device", requiredInterval: Long = -1): Mono<ManagedObject> {
    return list("type" to type, "pageSize" to "1")
        .take(1)
        .doOnNext {device->
            log.info("Device found $device")
        }
        .singleOrEmpty()
        .switchIfEmpty(

            create(
                Mono.just(
                    ManagedObject(
                        type = type,
                        name = name,
                        availability = RequiredAvailability(requiredInterval.toInt())
                    ).set(
                        "c8y_IsDevice",
                        emptyMap<Any, Any>()
                    ).set(Agent())
                )
            ).doOnNext {device->
                log.info("Device created $device")
            }
        )


}
