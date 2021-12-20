package io.c8y.scripts

import com.cumulocity.model.AgentFragment
import io.c8y.api.inventory.devicecontrol.Operation
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import java.time.Duration

fun main(){

    val current = Platform["staging-latest"]
    val api = current.rest().tenant()
        .ensureTenant("jaro-0")
        .map { current.forTenant(it) }
        .block()


    val agent = api.rest().inventory().list("fragmentType" to AgentFragment).blockFirst()!!;


    Flux.range(0,100)
        .delayElements(Duration.ofSeconds(5))
        .flatMap{
            api.rest().devicecontrol().create(Operation(
                deviceId = agent.id!!,
                agentId = agent.id!!,
            ).set("c8y_Restart",mapOf<String,Any>()))
        }
        .log()
        .blockLast()
}