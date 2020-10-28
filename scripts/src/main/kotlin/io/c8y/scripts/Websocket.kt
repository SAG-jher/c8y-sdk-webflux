package io.c8y.scripts

import io.c8y.api.management.realtime.RealtimeWebsocket
import io.c8y.config.Platform
import reactor.core.publisher.Mono


fun main() {


    val current = Platform["emea"]

    val realtime = RealtimeWebsocket(current, "cep/realtime")
    realtime
            .handshake()
            .doFinally {
                println("Handshake done")
                current.rest().inventory().list("fragmentType" to "c8y_IsDevice")
                    .map {
                        "/managedobjects/${it.id}"
                    }.collectList()
                    .flatMap {
                        realtime.subscribe(it)
                            .buffer(1000)
                            .map {
                                println(it.size)
                            }.subscribe()
                         Mono.empty<Void>()
                    }.subscribe()

            }
            .subscribe()




    Thread.sleep(100000000)


}
