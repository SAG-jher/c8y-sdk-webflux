package io.c8y.scripts

import com.cumulocity.model.AgentFragment
import io.c8y.api.inventory.devicecontrol.Operation
import io.c8y.api.inventory.ensureDevice
import io.c8y.api.inventory.smartrest.SmartREST.line
import io.c8y.api.inventory.smartrest.SmartREST.row
import io.c8y.api.usingTenant
import io.c8y.config.Platform
import reactor.core.scheduler.Schedulers
import java.util.concurrent.TimeUnit


fun main() {
    val mgmt = Platform["janssenpharma"]
    var api = mgmt//mgmt.usingTenant("jaro-0").block()


    val agent = api.rest().inventory().ensureDevice(type = "c8y_smartrest_realtime_test",fragments = arrayOf(AgentFragment to mapOf<String,Any>())).block()!!

    println("Agent: $agent ${agent.get()?.keys}")
    val templateId = "myTemplates1"
        api.rest().smartrest()
            .send(
                templateId,
                row("11", "508", "", "$.c8y_Restart", "$.id", "$.status")
            )
            .subscribe{
                println( it)
            }
    val deviceControl = api.rest().smartrest("notification/operations")
    val clientId = deviceControl
        .send(
            templateId,
            line("80")
        ).block().trim()
    println(clientId)
    deviceControl
        .send(
            templateId,
            line ("81,$clientId,/${agent.id}")
        ).block()

    deviceControl.send(templateId, line("83,$clientId"))
        .repeat()
        .subscribe {
            println(it)
            if(it.startsWith("86,")){
                System.exit(1)
            }
            val opId = it.split("\n").first { it.startsWith("508") }.split(",")[2]
            api.rest().devicecontrol().update(opId,"status" to "SUCCESSFUL").subscribe()

        }
    Schedulers.boundedElastic()
        .schedulePeriodically({
                              api.rest().devicecontrol().create(Operation(
                                  deviceId = agent.id,
                                  agentId = agent.id,

                              ).set("c8y_Restart", mapOf<String,Any>()))
                                  .subscribe()

        },10,1,TimeUnit.SECONDS)


    Thread.sleep(TimeUnit.MINUTES.toMillis(30))


}

