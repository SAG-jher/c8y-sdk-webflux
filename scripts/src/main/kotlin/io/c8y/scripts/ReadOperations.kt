package io.c8y.scripts

import io.c8y.api.BasicCredentials
import io.c8y.api.management.application.ApplicationType
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun main() {
    val current = Platform["local"]
    Mono.just(current).flatMap { api ->
        Flux.range(0, 50).flatMap {
            val rest = api.rest()
            rest.application().list()
                .filter { app -> app.type == ApplicationType.MICROSERVICE }
                .flatMap { app ->
                    println("Reading for ${app.id} - ${app.name}")
                    rest.application().bootstrapUser(app.id!!)
                        .map { bootstrap ->
                            current.withCredentials(
                                BasicCredentials(
                                    username = "${current.credentials.tenant}/${bootstrap.name}",
                                    password = bootstrap.password
                                )
                            )
                        }
                        .flatMap { app ->
                            app.rest().application().currentApplication().subscriptions()
                        }.doOnNext { subs ->
                            println("Loaded for ${app.id} - ${app.name} ${subs.users.size}")
                        }

                }
                .flatMapIterable { it.users }
        }
            .collectList()
    }.block()
}


