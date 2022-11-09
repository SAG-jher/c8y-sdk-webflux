package io.c8y.scripts

import io.c8y.api.*
import io.c8y.api.inventory.ManagedObject
import io.c8y.api.inventory.measurement.Measurement
import io.c8y.api.inventory.measurement.MeasurementCollection
import io.c8y.api.management.application.Application
import io.c8y.api.management.application.ApplicationType
import io.c8y.api.management.tenant.NewUser
import io.c8y.api.management.tenant.Tenant
import io.c8y.api.management.tenant.TenantApi
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.config.Platform
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.nio.file.Files
import java.nio.file.Files.newInputStream
import java.nio.file.Paths
import java.util.concurrent.ThreadLocalRandom
import io.c8y.scripts.support.log
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import java.nio.file.Path

fun main() {


    val platform = Platform["schindler"]

            Flux.range(0,4000).
                    concatMap {
                        platform.rest().user().create(
                            NewUser(
                                userName = "jaro-${it}"
                            )
                        )
                    }
                .blockLast()
}