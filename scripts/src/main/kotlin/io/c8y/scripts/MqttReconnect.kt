package io.c8y.scripts

import com.hivemq.client.internal.mqtt.message.publish.mqtt3.Mqtt3PublishViewBuilder.WillDefault
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe
import com.hivemq.client.mqtt.mqtt3.reactor.Mqtt3ReactorClient
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.TimeUnit

fun main() {
    val platformApi = Platform["staging-latest"]
    val mqttTest = platformApi.rest().tenant().ensureTenant(id = "jaro-mqtt").block()!!

    val api = platformApi.forTenant(mqttTest);

    val single = Schedulers.newBoundedElastic(50, 1000, "mqtt-clients")
    try {
        (0..500).forEach { id ->
            var deviceId = id
            val clientId = "d:test$deviceId"
            println("Strting new MqttClient $clientId ($id)")
            var mqtt: Mqtt3ReactorClient? = null

            single.schedulePeriodically({
                if (mqtt == null || mqtt?.state != MqttClientState.CONNECTED) {
                    println("Cient disconnected $clientId")
                    mqtt = api.mqtt3(clientId) {
                        it.willPublish(
                            WillDefault().topic("s/us")
                                .payload("200,c8y_Temperature,series,23".toByteArray())
                                .qos(MqttQos.EXACTLY_ONCE)
                                .retain(true)
                                .build()
                        )
                    }
                    mqtt!!.apply {
                        connect()
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
                            .subscribe()
                    }


                    mqtt?.run {
                        publishes(MqttGlobalPublishFilter.ALL)
                            .subscribe { message ->
                                println(
                                    "Received $clientId ${message.topic} message: ${
                                        String(
                                            message?.payload?.get()?.array()?: byteArrayOf()
                                        )
                                    }"
                                )
                            }
                    }

                } else {
                    println("Client connected $clientId ")
                    mqtt?.run { disconnect().subscribe() }
                    mqtt = null
                }
            }, 0, 1, TimeUnit.SECONDS)

        }

        Thread.sleep(TimeUnit.SECONDS.toMillis(30))
        println("Delete tenant $mqttTest.id")
        platformApi.rest().tenant().delete(mqttTest.id!!).block()
    } finally {
        single.dispose()
    }

}