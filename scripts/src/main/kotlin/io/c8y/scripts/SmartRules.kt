package io.c8y.scripts

import io.c8y.api.inventory.ensureDevice
import io.c8y.api.management.cep.SmartRule
import io.c8y.api.management.tenant.TenantApi
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import io.c8y.scripts.support.log
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

fun main() {
    val api = Platform["local"]

    val tenantApi = api.rest().tenant().ensureTenant("jaro-0")
        .map {
            api.forTenant(it)
        }.block()!!

    val tenantId = tenantApi.tenant.block()!!
    val cepId = api.rest().application().list("name" to "cep").blockFirst()!!.id!!
    val smartRuleId = api.rest().application().list("name" to "smartrule").blockFirst()!!.id!!
    if (!api.rest().tenant().isSubscribed(tenantId,cepId).block()!!){
        api.rest().tenant().subscribe(tenantId, smartRuleId).onErrorResume { Mono.empty<Void>() }.block()
        api.rest().tenant().subscribe(tenantId, cepId).onErrorResume { Mono.empty<Void>() }.block()
    }
    tenantApi.rest().inventory()
        .list("query" to "\$filter=(type+eq+'c8y_DeviceGroup')")

        .concatMap { group ->
            log.info("Group found {}", group.name)
            tenantApi.rest().inventory().listChildAssets(group.id!!).switchIfEmpty {
                Flux.range(0, 500)
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
                                tenantApi.rest().inventory().addChildAsset(group.id!!, device.id!!).map { device }
                            }

                    }
            }
                .collectList()
                .map { group }


        }

        .flatMap{ group ->
            tenantApi.rest().inventory()
                .listChildAssets(group.id!!)
                .map{Pair(group,it)}
        }
        .take(50)
        .buffer(30)
        .concatMap { smartRuleIndputs ->
            Flux.fromIterable(smartRuleIndputs).flatMap {
                val (group,device) = it

                tenantApi.rest().smartRules().create(
                    SmartRule(
                        context = mapOf(
                            "context" to "group",
                            "id" to group.id
                        ),
                        config = mapOf(
                            "alarmText" to "Test rule",
                            "alarmType" to "c8y_ThresholdAlarm",
                            "series" to "T",
                            "explicitVariant" to true,
                            "fragment" to "c8y_Temperature",
                            "redRangeMax" to 100,
                            "redRangeMin" to 90
                        ),
                        enabled = true,
                        enabledSources = listOf(device.id!!, group.id!!),
                        name = "Create alarm when measurement reaches explicit thresholds for device ${device.name}[${device.id}]",
                        ruleTemplateName = "explicitThresholdSmartRule",
                        type = "c8y_PrivateSmartRule"
                    )
                )
            }.collectList()



        }
        .last()
        .flatMapMany{tenantApi.rest().smartRules().list().map { it.id }.filter { it != null }}
        .collectList()
        .flatMapIterable { it }
        .distinct()
        .buffer(200)
        .concatMap { rules ->
            Flux.fromIterable(rules)
                .flatMap { tenantApi.rest().smartRules().delete(it!!).onErrorResume { Mono.empty() } }
                .collectList()

        }
        .collectList()
        .block()
}

