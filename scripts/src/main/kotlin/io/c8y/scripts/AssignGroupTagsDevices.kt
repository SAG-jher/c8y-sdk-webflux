package io.c8y.scripts

import io.c8y.api.inventory.*
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.api.support.Page
import io.c8y.config.Platform
import org.springframework.jmx.support.MetricType
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


fun main() {
    val api = Platform["staging-latest"]
    val rest = api.rest().tenant().ensureTenant("jaro-0").map{ tenant ->
        api.forTenant(tenant).let {
            it.withBaseUrl( it.url.scheme("https").port(null).build().toString())
        }
    }.block()
    val inventory = rest.rest().inventory()

//    inventory.list("type" to "c8y_DeviceSubgroup","withChildren" to "false")
//        .map { it.id }
//        .filter { it != null }
//        .map { it!! }
//        .collectList()
//        .flatMapIterable { it }
//        .concatMap {
//            inventory.delete(it)
//        }
//        .collectList()
//        .block()

    inventory.list("fragmentType" to "c8y_IsDevice","pageSize" to 2000,"withChildren" to "false")
        .index()
        .filter {
            it.t2.get()!!.keys.any { it.startsWith("group_") }
        }
        .concatMap {
            val update = it.t2.get()!!.keys.filter { it.startsWith("group_") }.map {  it to null}.toMap()
            inventory.update(it.t2.id!!, update)
//            inventory.update(it.t2.id!!, mapOf(
//                "group_${it.t1%50}" to null
//            ))
        }.blockLast()


}