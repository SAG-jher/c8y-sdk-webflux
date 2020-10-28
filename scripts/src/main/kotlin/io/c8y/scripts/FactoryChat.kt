package io.c8y.scripts

import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import io.c8y.scripts.support.log
import reactor.core.publisher.EmitterProcessor
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

fun main() {
    val api = Platform["local"]
    val microservice = "4436"


    val jaro0 = api.rest().tenant().ensureTenant("jaro-0").block()


    val scheduler = Schedulers.boundedElastic()

    val random = ThreadLocalRandom.current()

    val source = EmitterProcessor.create<Boolean>(100)

    val emitter = source.sink()
    (0 .. 5).forEach {
        scheduler.schedulePeriodically({
            val nextBoolean = random.nextBoolean()
            emitter.next(nextBoolean)
        },1,2,TimeUnit.SECONDS)

    }

   source
       .subscribeOn(Schedulers.elastic())
       .flatMap { subscribed->
        if(subscribed){
            api.rest().tenant().unsubscribe(jaro0.id!!,microservice)
        }else{
            api.rest().tenant().subscribe(jaro0.id!!,microservice)
        }
    }.onErrorContinue { failure, u ->
        if(!failure.message!!.let{it.contains("NOT assigned to the tenant") || it.contains("is already assigned to the tenant")} ) {
            log.info("failed {}", failure.message);
        }

    }.subscribe()
    Thread.sleep(1000000)
}

