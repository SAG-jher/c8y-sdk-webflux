package io.c8y.scripts
import io.c8y.scripts.support.log
import io.c8y.api.*
import io.c8y.api.inventory.alarm.Alarm
import io.c8y.api.inventory.alarm.Severity
import io.c8y.api.inventory.alarm.Status
import io.c8y.api.inventory.ensureDevice
import io.c8y.api.management.realtime.RealtimeWebsocketFlux
import io.c8y.api.management.realtime.channel
import io.c8y.api.management.realtime.data
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import io.reactivex.subjects.Subject
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

fun main() {


    val management = Platform["staging-2"]
    val current = management
        .rest().tenant().ensureTenant("jaro-0")
        .map {
            management.forTenant(it)
        }.block()
    val received = mutableMapOf<String, MutableSet<String>>()

    val realtime = RealtimeWebsocketFlux(current, "cep/realtime")
    realtime.handshake().block()


    Flux.range(0, 1)
        .flatMap {
            current.rest().inventory()
                .ensureDevice(type = "c8y_realtime_test$it")
                .flatMap {
                    current.rest().inventory().update(it.id!!, "c8y_RequiredAvailability" to mapOf("responseInterval" to TimeUnit.MINUTES.toSeconds(30)))
                }
                .map {
                    realtime.subscirbe("/alarms/${it.id}")
                        .log()
                        .subscribe()
                    it
                }

        }

        .concatMap { device->
            Flux.range(0,100)
                .delayElements(Duration.ofSeconds(30))
                .concatMap {
                    current.rest().alarm().list("status" to Status.ACTIVE)
                        .concatMap {
                            log.info("Active alarm {}",it)
                            current.rest().alarm().update(it.id!!, "status" to Status.CLEARED.name)
                        }.subscribe()
                    current.rest().alarm().create(
                        Alarm(
                            source = device.toReference(),
                            text = "My test alarm",
                            severity = Severity.MAJOR,
                            status = Status.ACTIVE,
                            type = "c8y_test_alarm"
                        )
                    ).doOnNext {
                        log.info("Alarm created {}",it)
                    }
                }

        }.blockLast()
//    Flux.range(0, 1)
//        .flatMap {
//            current.rest().inventory()
//                .ensureDevice(type = "c8y_realtime_test$it")
//
//        }
//        .concatMap {
//            val alarm = it.data["data"] as Map<String, Any>
//            val id = alarm["id"] as String
//            current.rest().alarm().update(id, "status" to Status.CLEARED)
//        }.doOnNext{
//            log.info("Alarm {}",it)
//        }






}
