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

    val options = rest.rest().tenant().systemOptions().list().sort { first, second ->
        first.category.compareTo(second.category).let { cmp->
            if (cmp != 0){
                cmp
            }else{
                first.key.compareTo(second.key)
            }
        }
    }
        .collectList().block()

    options.forEach {
        println("${it.category}:${it.key} = ${it.value}");
    }

    val version = rest.rest().tenant().systemOptions().get("system", "version").block()
    println("System version: ${version.value}")


}