package io.c8y.scripts

import com.hivemq.client.internal.mqtt.message.connect.connack.MqttConnAck
import com.hivemq.client.internal.mqtt.message.publish.mqtt3.Mqtt3PublishViewBuilder.WillDefault
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientSslConfig
import com.hivemq.client.mqtt.MqttClientTransportConfig
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe
import com.hivemq.client.mqtt.mqtt3.reactor.Mqtt3ReactorClient
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

fun main() {
    val platformApi = Platform["local"]
    val mqttTest = platformApi.rest().tenant().ensureTenant(id = "jaro-mqtt").block()!!
    val connected = ConcurrentHashMap<String,Boolean>()
    val api = platformApi.forTenant(mqttTest);

    val clients = Flux.range(0, 5000)
        .buffer(100)
        .concatMap {

            Flux.fromIterable(it)
                .flatMap { id ->
                    var deviceId = id
                    val clientId = "d:test$deviceId"
                    api.mqtt3(clientId) {
                        it.automaticReconnect(null)
                            .addConnectedListener {
                                println("$clientId ($id) connected ${connected.let { 
                                    it.put(clientId,true)
                                    it.size
                                }}")
                            }.addDisconnectedListener {
                                if(connected.containsKey(clientId)) {
                                    println(
                                        "$clientId ($id) disconnected ${
                                            connected.let {
                                                it.remove(clientId)
                                                it.size
                                            }
                                        } - ${it.cause.message}"
                                    )
                                }
                            }

                    }.let { mqtt ->
                        mqtt.connect().retry(10000).onErrorResume { Mono.empty() }
                            .map {
                                mqtt.run {
                                    publishes(MqttGlobalPublishFilter.ALL)
                                        .subscribe { message ->
                                            println(
                                                "Received $clientId ${message.topic} message: ${
                                                    String(
                                                        message?.payloadAsBytes ?: byteArrayOf()
                                                    )
                                                }"
                                            )
                                        }
                                }
                                mqtt
                            }.flatMap {
                                mqtt.subscribe(
                                    Mqtt3Subscribe.builder()
                                        .topicFilter("s/e")
                                        .qos(EXACTLY_ONCE)
                                        .build()
                                )
                             }
                            .flatMap {
                                mqtt.publish(
                                    Mono.just(
                                        Mqtt3Publish.builder()
                                            .topic("s/us")
                                            .payload("100".toByteArray())
                                            .qos(EXACTLY_ONCE)
                                            .build()
                                    )
                                ).toMono()
                            }.map {
                                mqtt
                            }
                    }


                }
        }.collectList()
        .block()
    println("All Clients started number of connected $connected")
    Thread.sleep(TimeUnit.SECONDS.toMillis(5))
    Flux.fromIterable(clients)
        .flatMap {
            it.disconnect()
        }.blockLast()
    println("Delete tenant $mqttTest.id")
    platformApi.rest().tenant().delete(mqttTest.id!!).block()


}