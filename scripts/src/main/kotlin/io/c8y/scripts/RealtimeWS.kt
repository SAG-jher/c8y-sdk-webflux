package io.c8y.scripts
import io.c8y.scripts.support.log
import io.c8y.api.*
import io.c8y.api.inventory.ensureDevice
import io.c8y.api.management.realtime.RealtimeWebsocketFlux
import io.c8y.api.management.realtime.channel
import io.c8y.api.management.realtime.data
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicLong

fun main() {


    val current = Platform["local"]
    val tenant = current.rest().tenant().ensureTenant("jaro-0").block()
    val c8y = current.forTenant(tenant)
    val received = mutableMapOf<String, MutableSet<String>>()

    val realtime = RealtimeWebsocketFlux(c8y, "cep/realtime")
    realtime.handshake().block()
    val numberOfMessages = AtomicLong()
    Flux.range(0, 200)
        .flatMap {
            c8y.rest().inventory()
                .ensureDevice(type = "c8y_realtime_test$it")
                .flatMapMany { device ->
                    realtime.subscirbe("/measurements/${device.id!!}")
                }
        }
        .subscribe {

            val measuremnt = it.data["data"] as Map<String, Any>
            val id = measuremnt["id"] as String
            synchronized(received) {
                val measurements = received.computeIfAbsent(it.channel, { mutableSetOf<String>() })
                if (id in measurements) {
                    log.error("Meassage already received {} for {}", id, it.channel)
                } else {
                    measurements.add(id)
                }
                val receivedMessages = numberOfMessages.incrementAndGet()
                if ((receivedMessages % 1000) == 0L) {
                    log.info("Message processed {}", receivedMessages)
                    if (receivedMessages > 100_000L) {
                        log.info("{}", received.mapValues { it.value.size })
                    }
                }

            }
        }




    Thread.sleep(100000000)


}
