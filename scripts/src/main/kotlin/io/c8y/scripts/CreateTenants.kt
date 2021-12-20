package io.c8y.scripts

import com.google.common.base.Stopwatch
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import org.apache.commons.lang.RandomStringUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

fun main() {
    val api = Platform["local"]

    val rest = api.rest()
    val tenant = rest.tenant()
    tenant.list()
//        .flatMap {
//            tenant.delete(it.id!!)
//        }
        .log()
        .blockLast()

    val cep = rest.application().list("name" to "cep").log().blockFirst()
    val echo= rest.application().list("name" to "echo-agent-server").log().blockFirst()


    tenant.list()
        .flatMap {t ->

            rest.tenant().subscribe(t.id!!,cep.id!!).map{t}.onErrorResume { Mono.fromCallable {t} }
                .then(
                rest.tenant().subscribe(t.id!!,echo.id!!).map{t}.onErrorResume { Mono.fromCallable {t} }
                )
        }
        .blockLast()
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