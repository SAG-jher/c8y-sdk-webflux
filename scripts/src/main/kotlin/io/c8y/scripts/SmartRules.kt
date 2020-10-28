package io.c8y.scripts

import io.c8y.api.inventory.ensureDevice
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import io.c8y.scripts.support.log
import reactor.core.publisher.Flux

fun main() {
    val api = Platform["staging-latest"]
    val tenantApi = api.rest().tenant().ensureTenant("jaro-5")
        .map {
            api.forTenant(it)
        }.block()
    tenantApi.rest().inventory()
        .list("query" to "\$filter=(type+eq+'c8y_DeviceGroup')")
//        .flatMap{group->
//            Flux.range(0,10)
//                .map { group to it }
//        }
        .concatMap { group ->
            log.info("Group found {}", group.name)
            Flux.range(0, 100000)
                .buffer(100)
                .concatMap { devices ->
                    Flux.fromIterable(devices)
                        .flatMap { deviceIndex ->
                            tenantApi.rest().inventory()
                                .ensureDevice(
                                    type = "c8y_smartrule_device_$deviceIndex",
                                    name = "SmartRule Device $deviceIndex"
                                )
                        }.flatMap { device ->
                            tenantApi.rest().inventory().addChildAsset(group.id!!, device.id!!)
                        }

                }.collectList()
                .flatMapIterable { it }


        }
//        .concatMap { groupWithIndex ->
//            val group = groupWithIndex.first
//            val index = groupWithIndex.second
//            tenantApi.rest().inventory()
//                .listChildAssets(group.id!!)
//                .buffer(10)
//                .concatMap {
//                    Flux.fromIterable(it)
//                        .flatMap { device ->
//                            tenantApi.rest().smartRules().create(
//                                SmartRule(
//                                    context = mapOf(
//                                        "context" to "group",
//                                        "id" to group.id
//                                    ),
//                                    config = mapOf(
//                                        "alarmText" to "Test rule",
//                                        "alarmType" to "c8y_ThresholdAlarm",
//                                        "series" to "T",
//                                        "explicitVariant" to true,
//                                        "fragment" to "c8y_Temperature",
//                                        "redRangeMax" to 100,
//                                        "redRangeMin" to 90
//                                    ),
//                                    enabled = true,
//                                    enabledSources = listOf(device.id!!, group.id),
//                                    name = "Create alarm when measurement reaches explicit thresholds for device ${device.name}[${device.id}] ${index}",
//                                    ruleTemplateName = "explicitThresholdSmartRule",
//                                    type = "c8y_PrivateSmartRule"
//                                )
//                            )
//
//                        }
//                }
//                .collectList()

//        }
//        .flatMapIterable { it }
//        .collectList()
//        .delayElement(Duration.ofMinutes(3))
//        .flatMapIterable {
//            it.map { it.id }.filter { it != null }.toSet()
//        }
//        .mergeWith(tenantApi.rest().smartRules().list().map { it.id }.filter { it != null })
//    tenantApi.rest().smartRules().list().map { it.id }.filter { it != null }
//        .collectList()
//        .flatMapIterable { it }
//        .distinct()
//        .buffer(5)
//        .concatMap { rules ->
//            Flux.fromIterable(rules)
//                .flatMap { tenantApi.rest().smartRules().delete(it!!).onErrorResume { Mono.empty() } }
//                .collectList()
//
//        }
        .collectList()
        .block()
}