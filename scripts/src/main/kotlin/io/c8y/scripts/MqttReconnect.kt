package io.c8y.scripts

import com.hivemq.client.internal.mqtt.message.publish.mqtt3.Mqtt3PublishViewBuilder.WillDefault
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3PublishResult
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe
import com.hivemq.client.mqtt.mqtt3.reactor.Mqtt3ReactorClient
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

fun main() {
    val platformApi = Platform["local"]
    val mqttTest = platformApi.rest().tenant().ensureTenant(id = "jaro-0").block()!!
    val activeClients =ConcurrentHashMap<String,Mqtt3ReactorClient>()
    val api = platformApi.forTenant(mqttTest);

    val single = Schedulers.newBoundedElastic(50, 1000, "mqtt-clients")
    single.schedulePeriodically({
         println("Active clients : ${activeClients.size}")

    },10,10,TimeUnit.SECONDS)
    try {
        (0..30000).forEach { id ->
            val deviceId = id
            val clientId = "d:test$deviceId"

            single.schedulePeriodically({
                if(!activeClients.containsKey(clientId)){
                       val mqtt = api.mqtt3(clientId) {
                            it.willPublish(
                                WillDefault().topic("s/us")
                                    .payload("200,c8y_Temperature,series,23".toByteArray())
                                    .qos(MqttQos.EXACTLY_ONCE)
                                    .retain(true)
                                    .build()
                            )
                        }
                        mqtt.reconnectToC8y()
                            .subscribe {
                                activeClients.put(clientId, mqtt!!)
                            }


                        mqtt.run {
                            publishes(MqttGlobalPublishFilter.ALL)
                                .subscribe { message ->
                                    println(
                                        "Received $clientId ${message.topic} message: ${
                                            String(
                                                message?.payload?.get()?.array() ?: byteArrayOf()
                                            )
                                        }"
                                    )
                                }
                        }
                } else {

                }
            }, id.toLong(), 30, TimeUnit.SECONDS)

        }

        Thread.sleep(TimeUnit.MINUTES.toMillis(30))
        println("Delete tenant $mqttTest.id")
//        platformApi.rest().tenant().delete(mqttTest.id!!).block()
    } finally {
        single.dispose()
    }

}

private fun Mqtt3ReactorClient.reconnectToC8y( ): Mono<Mqtt3PublishResult> {
    return this.connect()
            .then(
                subscribe(Mqtt3Subscribe.builder().topicFilter("s/e").qos(MqttQos.EXACTLY_ONCE).build())
            )
            .then(
                publish(
                    Mono.just(
                        Mqtt3Publish.builder().topic("s/us").payload("100".toByteArray())
                            .qos(MqttQos.EXACTLY_ONCE).build()
                    )
                ).toMono()
            )

}