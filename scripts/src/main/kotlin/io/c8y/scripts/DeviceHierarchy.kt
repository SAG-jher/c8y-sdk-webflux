package io.c8y.scripts

import io.c8y.api.inventory.*
import io.c8y.api.inventory.alarm.Alarm
import io.c8y.api.inventory.alarm.Severity
import io.c8y.api.inventory.alarm.Status
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.api.support.Page
import io.c8y.config.Platform
import org.springframework.jmx.support.MetricType
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


fun main() {
    val api = Platform["local"]
    val rest = api.rest().tenant().ensureTenant("jaro-0").map{ tenant ->
        api.forTenant(tenant)
    }.block()
    val inventory = rest.rest().inventory()

    val byName = inventory.list( "pageSize" to "2000")
        .filter { it.name != null }
        .map { mo ->
            mo.copy(childAssets = ManagedObjectRefCollection(references = listOf())) to (mo.childAssets?: ManagedObjectRefCollection(references = listOf()))
                .map { it.managedObject }
                .map { it.id!! }
                .toSet()

        }
        .collectMap { it.first.name }
        .block()!!




    Flux.range(0, 2)
        .concatMap { index ->
            val name = "${index.toString().reversed()} Top level group $index"
            if (!byName.containsKey(name)) {
                inventory.ensureGroup(name).map { it to mutableListOf<String>() }
            } else {
                Mono.just(byName[name]!!)
            }
        }
        .log()
        .concatMap { group ->
            if(group.first.name == "0 Top level group 0") {
                Flux.range(0, 5)
                    .map { group to it }
            }else{
                Flux.range(0,100)
                    .map {group to it }

            }
        }
        .concatMap { groupWithIndex ->
            val (group, index) = groupWithIndex
            val name = "${index.toString().reversed()} SubGroup ${group.first.id} $index"
            if (!byName.containsKey(name)) {
                inventory.ensureSubGroup(name)
                    .map { group to (it to mutableListOf<String>()) }
            } else {
                Mono.just(group to byName[name]!!)
            }

        }
        .concatMap { groupWithSubgroup ->
            val (group, subgroup) = groupWithSubgroup
            if (group.second.contains(subgroup.first.id)) {
                Mono.just(subgroup)
            } else {
                inventory.addChildAsset(group.first.id!!, subgroup.first.id!!)
                    .map { subgroup }
            }
        }
        .flatMap { subgroup ->
            if((subgroup.first.id!!.toInt() / 3 )%2 == 0) {
                Flux.range(0, 200000).map { subgroup to it }
            }else{
                Flux.range(0, 500).map { subgroup to it }
            }
        }
        .buffer(10)
        .concatMap { groups ->
            Flux.fromIterable(groups)
                .flatMap { groupWithIndex ->
                    val (subgroup, deviceIndex) = groupWithIndex
                    val name = "${deviceIndex.toString().reversed()} ${subgroup.first.name?.replace(" ", "_")}_${subgroup.first.id}_$deviceIndex"

                    if (!byName.containsKey(name)) {
                        inventory.ensureDevice(name = name, type = name)
                            .map { subgroup to it }
                    } else {
                        Mono.just(subgroup to  byName[name]!!.first)
                    }



                }.collectList()
        }
        .flatMapIterable { it }
        .buffer(10)
        .concatMap {
            Flux.fromIterable(it)
                .concatMap {
                    if (it.first.second.contains(it.second.id!!)) {
                        Mono.just(it.second)
                    } else {
                        Mono.just(it)
                            .flatMap { subgroupWithDevice ->
                                val (subgroup, device) = subgroupWithDevice
                                inventory
                                    .addChildAsset(subgroup.first.id!!, device.id!!)
                                    .map { device }
                            }
                    }

                }

        }
        .log()

        .blockLast()




}
