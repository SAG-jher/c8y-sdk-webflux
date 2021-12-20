package io.c8y.scripts

import io.c8y.api.inventory.listDevices
import io.c8y.api.inventory.listGroups
import io.c8y.api.usingTenant
import io.c8y.config.Platform


fun main(){
    val local = Platform["local"].usingTenant("jaro-0").block()
    val devices = local.rest().inventory().listDevices().count().block()

    val groups = local.rest().inventory().listGroups().count().block()
    val largestGroups = local.rest().inventory().listGroups().flatMap { group ->
        local.rest().inventory().listChildAssets(group.id!!)
            .count()
            .map { children ->
                "${group.name}(${group.id})" to children
            }
    }.sort { first, second -> second.second.compareTo(first.second) }

    .take(10)
    .collectList()
    .map {
        it
            .joinToString("\n") { "${it.first} => ${it.second}" }
    }
    .block()
    println("Devices ${devices}")
    println("Groups ${groups}")

    println("Larges $largestGroups")
}