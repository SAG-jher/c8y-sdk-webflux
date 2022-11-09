package io.c8y.scripts

import io.c8y.config.Platform
import org.springframework.web.reactive.function.client.WebClientResponseException

fun main() {
    val platform = Platform["staging-latest"]
    val rest = platform.rest()

    val tenantApi = rest.tenant()


    try {
        val app = rest.application().list().collectList().block()
        app.filter {
            it.name!!.contains("actility")
        }.forEach {
            println("${it.name} - ${it.owner?.tenant?.id}")
        }
        val options = rest.tenant().options().list().collectList().block()
        options.filter { it.key.contains("actility") }.forEach {
            println("${it.category}.${it.key} - ${it.value}")
        }


//        val result = tenantApi.tenants(20,true)
//            .concatMap {
//                log.info("Unsubscribe tenant {} from {}",it.id,app.id)
//                tenantApi.unsubscribe(it.id!!,app.id!!).onErrorResume { Mono.empty() }

//                    .flatMap { tenant ->
//                        Mono.just(tenant)
//                            .flatMap {
//                                rest.application().list("name" to "echo-agent-server")
//                                    .take(1)
//                                    .single()
//                                    .flatMap { app ->
//                                        rest.tenant().subscribe(tenant = tenant.id!!, application = app.id!!)
//                                            .onErrorResume { Mono.empty() }
//                                    }.map { tenant }
//                                    .defaultIfEmpty(tenant)
//
//                            }
//                            .map { platform.forTenant(tenant) }
//                            .toFlux()
//                            .concatMap { api ->
//                                val echo = api.rest().application().clientFor("echo-agent-server")
//                                Flux.range(0, 1000000)
//                                    .delayElements(Duration.ofMillis(500))
//                                    .flatMap {
//                                        echo.get().retrieve().onRawStatus(
//                                                { true },
//                                                { Mono.empty() }
//                                            ).toEntity(String::class.java)
//                                            .flatMap {
//                                                if (it.statusCodeValue == 401) {
//                                                    api.rest().user().currentUser().get()
//                                                        .onErrorResume { Mono.empty() }
//                                                        .flatMap {
//                                                            api.tenant
//                                                        }.map {
//                                                            throw RuntimeException("failed for not disabled tenant ${it}")
//                                                        }
//
//                                                } else {
//                                                    Mono.empty()
//                                                }
//
//                                            }
//                                    }
//
//                            }
//
//
//                            .map { tenant }
//                    }


//                    .flatMap { tenantApi.delete(it.id!!).onErrorResume { Mono.empty() } }
//
//                Flux.fromIterable(tenants)
//                    .flatMap { tenant ->
//                        val platformApi = platform.forTenant(tenant)
//                        val tenantRest = platformApi
//                            .rest()
//
//                        val mqtt = platformApi.mqtt("test")
//
//                        Mono.create<MqttAsyncClient> { sink ->
//                            sink.onRequest {
//                                mqtt.publish("s/us", MqttMessage().apply {
//                                    payload = "100".toByteArray()
//                                    qos = 2
//                                }).actionCallback = object : IMqttActionListener {
//                                    override fun onSuccess(asyncActionToken: IMqttToken?) {
//                                        sink.success(mqtt)
//                                    }
//
//                                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//                                        sink.error(exception)
//                                    }
//                                }
//                            }
//                        }
//                    }.flatMap { mqtt ->
//                        Flux.range(0, 10000).delayElements(Duration.ofSeconds(5))
//                            .concatMap { value ->
//                                Mono.create<Void> { sink ->
//                                    sink.onRequest {
//                                        mqtt.publish("s/us", MqttMessage().apply {
//                                            payload = "200,c8y_Temperature,T,${value % 1000}".toByteArray()
//                                            qos = 2
//                                        }).actionCallback = object : IMqttActionListener {
//                                            override fun onSuccess(asyncActionToken: IMqttToken?) {
//                                                sink.success()
//                                            }
//
//                                            override fun onFailure(
//                                                asyncActionToken: IMqttToken?,
//                                                exception: Throwable?
//                                            ) {
//                                                sink.error(exception)
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            .last()
//                            .flatMap {
//                                Mono.create<Void> { sink ->
//                                    sink.onRequest {
//                                        mqtt.disconnect().actionCallback = object : IMqttActionListener {
//                                            override fun onSuccess(asyncActionToken: IMqttToken?) {
//                                                sink.success()
//                                            }
//
//                                            override fun onFailure(
//                                                asyncActionToken: IMqttToken?,
//                                                exception: Throwable?
//                                            ) {
//                                                sink.error(exception)
//                                            }
//                                        }
//                                    }
//                                }
//                            }


//                        ensureDevice(platformApi).flatMapMany { device ->
//                            Flux.range(0, 10000)
//                                .delayElements(Duration.ofSeconds(5))
//                                .concatMap {
//                                    tenantRest.measurement().create(
//                                        Mono.just(
//                                            Measurement(
//                                                source = ManagedObjectReference(device.id!!),
//                                                type = "c8y_Temperature"
//
//
//                                            ).set(
//                                                "c8y_Temperature",
//                                                MeasurementValue(System.currentTimeMillis() % 1000, "T")
//                                            )
//                                        )
//                                    )
//                                }
//                        }


//
//                        tenantApi.unsubscribe(tenant.id!!, "839")
//                            .onErrorResume {
//                                Mono.empty()
//                            }
//                            .then(tenantApi.subscribe(tenant.id!!, "839")
//                                .onErrorResume {
//                                    Mono.empty()
//                                })
//                            .map { it }
//                    }
//            }.collectList().block()
//            .concatMap {
//                Flux.fromIterable(it)
//                    .map { it to Platform.from(it) }
//                    .flatMap { context ->
//                        val (tenant, api) = context
//
//
//                        val name = "app-for-${tenant.id}"
//                        api.rest().application().list("name" to name)
//                            .collectList()
//                            .flatMap {
//                                if(it.isEmpty()) {
//                                    api.rest().application().create(
//                                        Mono.just(
//                                            Application(
//                                                name = name,
//                                                key = "${name}-key",
//                                                type = ApplicationType.MICROSERVICE
//                                            )
//                                        )
//                                    ).map { context.first }
//                                }else{
//                                    Mono.just(context.first)
//                                }
//                            }
//                    }
//            }


//        println(result)
    } catch (ex: WebClientResponseException) {
        println("${ex.statusCode} ${ex.request.method} ${ex.request.uri} ${ex.responseBodyAsString}")
    }
}
