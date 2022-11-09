package io.c8y.scripts

import io.c8y.api.inventory.devicecontrol.BulkOperation
import io.c8y.api.inventory.ensureGroup
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import java.time.OffsetDateTime

fun main() {
    val api = Platform["schindler"]


    try {
        println(
            api.rest().devicecontrol().bulk().create(
                BulkOperation(
                    groupId = api.rest().inventory().list("fragmentType" to "c8y_IsGroup").blockFirst()!!.id!!,
                    creationRamp = 1,
                    startDate = OffsetDateTime.now().plusSeconds(30),
                    operationPrototype = mapOf(
                        "c8y_Restart" to emptyMap<Any, Any>()
                    )
                )
            ).block()
        )
    } catch (ex: Exception) {
        println(ex.message);
        System.out.flush()
    }


}