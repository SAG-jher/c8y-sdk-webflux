package io.c8y.scripts

import com.spotify.docker.client.DefaultDockerClient
import io.c8y.api.management.application.Application
import io.c8y.api.management.application.ApplicationType
import io.c8y.config.Platform
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun main() {
    val rest = Platform["default"].rest()

    val tenantApi = rest.tenant()

    val applicationApi = rest.application()

    val boundedElastic = Schedulers.boundedElastic()
    Flux.fromIterable(
        arrayListOf<String>(
            "mongo",
            "centos",
            "ubuntu",
            "debian",
            "redis",
            "postgres",
            "node",
            "nginx"
        )
    )
        .map { "$it:latest" }
        .flatMap { image ->

            applicationApi.create(
                    Application(
                        name = "test-" + System.currentTimeMillis(),
                        type = ApplicationType.MICROSERVICE,
                        key = "test-" + System.currentTimeMillis()
                    )
            ).map { it to image }
        }
        .subscribeOn(boundedElastic)
        .map {
            it.first to downloadImage(it.second)
        }
        .subscribeOn(boundedElastic)
        .flatMap { upload ->
            applicationApi.upload(upload.first.id!!,  upload.second)
        }

        .collectList()
        .block()


}


private fun downloadImage(
    image: String
): Flux<DataBuffer> {
    val log = LoggerFactory.getLogger("downloadImage")

    return DataBufferUtils.readInputStream(  {
        DefaultDockerClient.fromEnv().build().use { docker ->
            log.info("Pulling image {}", image)
            docker.pull(image)
            log.info("Saving image {}", image)
            val zipFile = Paths.get("${image}.zip")
            if(Files.exists(zipFile)){
                return@readInputStream Files.newInputStream(zipFile)
            }

            docker.save(image).use { imageTar ->
                log.info("Image saved {}", image)
                ZipOutputStream(Files.newOutputStream(zipFile)).use { out ->
                    out.putNextEntry(ZipEntry("image.tar"))
                    IOUtils.copy(imageTar, out)
                    out.closeEntry()
                    out.putNextEntry(ZipEntry("cumulocity.json"))
                    out.write(
                        """
                                {
                                  "apiVersion": "1",
                                  "version": "1.0.0",
                                  "provider": {
                                    "name": "Cumulocity GmbH"
                                  },
                                  "isolation": "MULTI_TENANT",
                                  "requiredRoles": [
                                    "ROLE_INVENTORY_READ",
                                    "ROLE_INVENTORY_ADMIN",
                                    "ROLE_IDENTITY_READ",
                                    "ROLE_OPTION_MANAGEMENT_ADMIN",
                                    "ROLE_OPTION_MANAGEMENT_READ",
                                    "ROLE_EVENT_ADMIN",
                                    "ROLE_MEASUREMENT_ADMIN",
                                    "ROLE_MEASUREMENT_READ",
                                    "ROLE_TENANT_MANAGEMENT_READ"
                                  ],
                                  "roles": [
                                  ],
                                  "livenessProbe":{
                                    "httpGet":{
                                      "path": "/health",
                                      "port": 80

                                    },
                                    "initialDelaySeconds": 200,
                                    "periodSeconds": 10
                                  },
                                  "readinessProbe":{
                                    "httpGet":{
                                      "path": "/health",
                                      "port": 80

                                    },
                                    "initialDelaySeconds": 220,
                                    "periodSeconds": 10
                                  }
                                }
                            """.trimIndent().toByteArray()
                    )


                    out.closeEntry()
                }

                return@readInputStream Files.newInputStream(zipFile)
            }

        }
    },DefaultDataBufferFactory.sharedInstance,1024)
};






