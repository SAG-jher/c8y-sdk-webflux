package io.c8y.scripts

import io.c8y.api.management.tenant.Tenant
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.api.management.tenant.tenants
import io.c8y.config.Platform
import org.apache.commons.lang3.RandomStringUtils

fun main() {
    val api = Platform["staging-latest"]
    api.rest().tenant().ensureTenant("jaro-0").block()
    api.rest().tenant().update("jaro-0",
        "allowCreateTenants" to true
        ).block()

    val rest = api.forTenant(Tenant("jaro-0")).rest()
    val tenant = rest.tenant()
//
//    tenant.tenants(300,create = true,prefix="subtenant-of-jaro")
//        .filter { it.id!! != "jaro-0" }
//        .concatMap {
//            tenant.update(it.id!!,
//                "customProperties" to mapOf<String,Any>(
//                    *(0 until  1000).map {index->
//                        "custom_property_$index" to RandomStringUtils.randomAlphabetic(300)!!
//                    }.toTypedArray()
//                )
//            )
//        }.blockLast()

//    tenant.list()
//        .flatMap {
//            tenant.delete(it.id!!)
//        }
//        .log()
//        .blockLast()
//
//    val cep = rest.application().list("name" to "cep").log().blockFirst()
//    val echo= rest.application().list("name" to "echo-agent-server").log().blockFirst()


//    tenant.list()
//        .flatMap {t ->
//
//            rest.tenant().subscribe(t.id!!,cep.id!!).map{t}.onErrorResume { Mono.fromCallable {t} }
//                .then(
//                rest.tenant().subscribe(t.id!!,echo.id!!).map{t}.onErrorResume { Mono.fromCallable {t} }
//                )
//        }
//        .blockLast()

//    api.rest().tenant().list()
//        .filter { it.id!!.startsWith("sampletenant-") }
//        .filter { it.id!! != "jaro-0" }
//        .concatMap {
//            api.rest().tenant().delete(it.id!!)
//        }
//        .blockLast()

//    api.forTenant( api.rest().tenant().ensureTenant("jaro-0").block())
//        .rest().inventory().list()
//        .log()
//        .blockLast()
//    Flux.range(0, 1000)
//        .buffer(7)
//        .concatMap {
//            Flux.fromIterable(it)
//                .flatMap {
//                    val stopwatch = Stopwatch.createStarted()
//                    val tenantId = "tenant-creation-${RandomStringUtils.randomNumeric(5)}"
//                    tenant.ensureTenant(tenantId)
//                        .map {
//                            tenantId to stopwatch.stop()
//                        }
//                }.flatMap { progress ->
//                    tenant.delete(progress.first)
//                        .map {
//                            progress
//                        }
//                }.filter { progress->
//                    progress.second.elapsed(TimeUnit.SECONDS) > 15
//                }.
//                log()
//        }.blockLast()

}