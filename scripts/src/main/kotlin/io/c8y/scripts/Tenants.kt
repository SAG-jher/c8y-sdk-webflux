package io.c8y.scripts

import io.c8y.api.*
import io.c8y.api.inventory.ManagedObject
import io.c8y.api.inventory.measurement.Measurement
import io.c8y.api.inventory.measurement.MeasurementCollection
import io.c8y.api.management.application.Application
import io.c8y.api.management.application.ApplicationType
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


    val platform = Platform["local"]
    platform.rest().tenant().ensureTenant("jaro-0")
        .map {
            platform.forTenant(it)
        }.flatMapIterable { api ->
            (0..100000).map { api }
        }.concatMap { api ->
            api.createMicroserviceForTenant()
                .flatMap { app ->
                    Flux.range(0, 100)
                        .map { ThreadLocalRandom.current().nextBoolean() }
                        .flatMap {
                            if (it) {
                                log.info("Subscribe ${app.id}")
                                api.rest().tenant().subscribe(application = app.id!!).onErrorResume { ex ->
                                    when (ex) {
                                        is RestException -> {
                                            if (ex.status.is5xxServerError)
                                                Mono.defer {
                                                    throw ex
                                                } else Mono.empty()
                                        }
                                        else -> {
                                            Mono.empty()
                                        }
                                    }


                                }
                            }else{
                                    log.info("Unsubscribe ${app.id}")
                                    api.rest().tenant().unsubscribe(application = app.id!!).onErrorResume { ex ->
                                        when (ex) {
                                            is RestException -> {
                                                if (ex.status.is5xxServerError)
                                                    Mono.defer {
                                                        throw ex
                                                    } else Mono.empty()
                                            }
                                            else -> {
                                                Mono.empty()
                                            }
                                        }


                                    }
                                }
                            }.collectList()
                                .map { app }
                                .switchIfEmpty(Mono.just(app))

                        }
                        .flatMap { app ->
                            log.info("delete ${app.id}")
                            api.rest().application().delete(app.id!!)
                        }.doOnEach {
                            log.info(" Event $it")
                        }

                }.blockLast()


        }

    fun PlatformApi.createMicroserviceForTenant(): Mono<Application> {
        return this.tenant.flatMap { tenant ->

            val name = "app-for-${tenant}"
            this.rest().application().list("name" to name)
                .take(1)
                .singleOrEmpty()
                .doOnNext { app ->
                    log.info("Found app  ${app.id} with name ${app.name}")
                }
                .switchIfEmpty {
                    log.info("Creating app as it's not found with name: $name")
                    this.rest().application()
                        .create(
                                Application(
                                    name = name,
                                    key = "${name}-key",
                                    type = ApplicationType.MICROSERVICE
                                )
                        )
                }.flatMap { app ->
                    log.info("Assign binary to  ${app.id}")
                    if (app.activeVersionId == null) {
                        log.info("Uploading binary ${app.id}")
                        this.rest().application().upload(app.id!!, Mono.defer<Path> {
                            Files.newDirectoryStream(
                                Paths.get(System.getenv("CUMULOCITY_HOME"), "/agents/echo/target/"),
                                "echo-agent-server-*.zip"
                            ).use{
                                Mono.just(it.first())
                            }

                        }.flatMapMany {
                            DataBufferUtils.read(it,DefaultDataBufferFactory.sharedInstance,1024*1024)
                         })
                            .map { app }
                            .switchIfEmpty(Mono.just(app))
                    } else {
                        log.info("Binary already uploaded ${app.id}")
                        Mono.just(app)
                    }
                }

        }


    }

    private fun listTenants(tenantApi: TenantApi, filter: (Tenant) -> Boolean): Flux<Tenant> {
        return tenantApi.list("pageSize" to 1000)
            .filter(filter)
    }


    private fun createMeasurements(
        platform: PlatformApi,
        device: ManagedObject,
        numberOfMeasurements: Int = 100
    ): Mono<MeasurementCollection> {
        return platform.rest().measurement().createMany(
                MeasurementCollection(
                    measurements = (1..numberOfMeasurements).map { value ->
                        Measurement(source = device.toReference(), type = "measurement").set(
                            "c8y_Temperature",
                            mapOf("T" to mapOf("value" to value, "unit" to "C"))
                        )
                    }
                )
        )
    }

    private fun createMeasurement(platform: PlatformApi, device: ManagedObject): Mono<Measurement> {

        return platform.rest().measurement().create(
                Measurement(source = device.toReference(), type = "measurement").set(
                    "c8y_Temperature",
                    mapOf("T" to mapOf("value" to 10, "unit" to "C"))
                )
        )
    }

    private fun refreshSubscription(platform: PlatformApi): Mono<PlatformApi> {
        val rest = platform.rest()
        return rest.cep()
            .refresh().then(Mono.just(platform))
    }


    private fun createCepModule(platform: PlatformApi): Mono<PlatformApi> {
        return platform.rest().cep().upload(
            Mono.just(
                """
                    module simple_test;

                    insert into CreateAlarm
                    select
                        m.source as source,
                        "ACTIVE" as status,
                        current_timestamp().toDate() as time,
                        "cep-test" as type,
                        "MAJOR" as severity,
                        "New measurement "  as text
                    from
                        MeasurementCreated m;
                """.trimIndent().byteInputStream()
            )
        ).onErrorResume { Mono.empty() }
            .then(Mono.just(platform))
    }

