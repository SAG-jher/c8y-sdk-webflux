package io.c8y.scripts

import io.c8y.api.management.application.ensureApplication
import io.c8y.api.management.tenant.ensureTenant
import io.c8y.api.management.tenant.from
import io.c8y.config.Platform
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.CorePublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


fun main() {
    val local = Platform["local"]
    val rest = local.rest()
    val api = rest.tenant().ensureTenant("jaro-0")
        .map {
            it.from(local)
        }.block()

    val images = WebClient.builder()
        .filter(
            ExchangeFilterFunctions.basicAuthentication(
                "K8Simages",
                "K8S^imAgEs5000%"
            )
        )
        .baseUrl("https://resources.cumulocity.com/kubernetes-images/")
        .build()
    val imagesToDownload = """
       /resources/kubernetes-images/cep-small-1010.1.0-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1007.0.27.zip
/resources/kubernetes-images/cep-small-1007.12.0.zip
/resources/kubernetes-images/cep-small-1005.7.18.zip
/resources/kubernetes-images/cep-small-1010.0.1-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1010.0.0.zip
/resources/kubernetes-images/cep-small-1007.14.0.zip
/resources/kubernetes-images/cep-small-1009.0.7-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1005.7.16.zip
/resources/kubernetes-images/cep-small-1009.0.4.zip
/resources/kubernetes-images/cep-small-9.20.19.zip
/resources/kubernetes-images/cep-small-1010.2.1-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-9.20.21.zip
/resources/kubernetes-images/cep-small-1009.0.9-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1006.0.25.zip
/resources/kubernetes-images/cep-small-1009.0.8.zip
/resources/kubernetes-images/cep-small-9.20.17.zip
/resources/kubernetes-images/cep-small-1009.8.0.zip
/resources/kubernetes-images/cep-small-9.20.18.zip
/resources/kubernetes-images/cep-small-1007.0.31.zip
/resources/kubernetes-images/cep-small-1007.0.34.zip
/resources/kubernetes-images/cep-small-1007.0.22.zip
/resources/kubernetes-images/cep-small-1006.0.23.zip
/resources/kubernetes-images/cep-small-1007.10.0.zip
/resources/kubernetes-images/cep-small-1009.14.0-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1006.0.0.zip
/resources/kubernetes-images/cep-small-1007.0.39-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1007.0.24.zip
/resources/kubernetes-images/cep-small-1006.6.34.zip
/resources/kubernetes-images/cep-small-1006.6.7.zip
/resources/kubernetes-images/cep-small-1009.2.0.zip
/resources/kubernetes-images/cep-small-9.16.16.zip
/resources/kubernetes-images/cep-small-1006.0.5.zip
/resources/kubernetes-images/cep-small-1006.6.21.zip
/resources/kubernetes-images/cep-small-1006.6.5.zip
/resources/kubernetes-images/cep-small-1009.0.9.zip
/resources/kubernetes-images/cep-small-1006.0.21.zip
/resources/kubernetes-images/cep-small-1006.6.27.zip
/resources/kubernetes-images/cep-small-1006.0.8.zip
/resources/kubernetes-images/cep-small-1009.0.12.zip
/resources/kubernetes-images/cep-small-1006.6.22.zip
/resources/kubernetes-images/cep-small-9.20.15.zip
/resources/kubernetes-images/cep-small-1009.0.10-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1009.0.11-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-9.20.13.zip
/resources/kubernetes-images/cep-small-1006.0.12.zip
/resources/kubernetes-images/cep-small-1007.0.30.zip
/resources/kubernetes-images/cep-small-1006.6.37.zip
/resources/kubernetes-images/cep-small-1005.7.15.zip
/resources/kubernetes-images/cep-small-1007.0.29.zip
/resources/kubernetes-images/cep-small-1006.0.4.zip
/resources/kubernetes-images/cep-small-1007.18.0.zip
/resources/kubernetes-images/cep-small-1005.0.7.zip
/resources/kubernetes-images/cep-small-1009.0.2.zip
/resources/kubernetes-images/cep-small-1006.0.9.zip
/resources/kubernetes-images/cep-small-1006.6.33.zip
/resources/kubernetes-images/cep-small-1006.0.1.zip
/resources/kubernetes-images/cep-small-1004.6.20.zip
/resources/kubernetes-images/cep-small-1009.11.0-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1007.0.33.zip
/resources/kubernetes-images/cep-small-1009.0.0.zip
/resources/kubernetes-images/cep-small-1007.0.32.zip
/resources/kubernetes-images/cep-small-1006.6.28.zip
/resources/kubernetes-images/cep-small-1009.10.1-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1009.0.7.zip
/resources/kubernetes-images/cep-small-1006.0.28.zip
/resources/kubernetes-images/cep-small-1009.0.6.zip
/resources/kubernetes-images/cep-small-1005.7.13.zip
/resources/kubernetes-images/cep-small-1009.12.0.zip
/resources/kubernetes-images/cep-small-9.20.14.zip
/resources/kubernetes-images/cep-small-1009.13.1-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1009.9.0.zip
/resources/kubernetes-images/cep-small-1009.12.1-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-9.20.12.zip
/resources/kubernetes-images/cep-small-1006.0.13.zip
/resources/kubernetes-images/cep-small-1007.0.40-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1006.0.0-2.zip
/resources/kubernetes-images/cep-small-1006.6.1.zip
/resources/kubernetes-images/cep-small-1009.10.0.zip
/resources/kubernetes-images/cep-small-1007.11.0.zip
/resources/kubernetes-images/cep-small-1006.6.20.zip
/resources/kubernetes-images/cep-small-9.20.16.zip
/resources/kubernetes-images/cep-small-1007.8.0.zip
/resources/kubernetes-images/cep-small-1006.0.22.zip
/resources/kubernetes-images/cep-small-1004.0.20.zip
/resources/kubernetes-images/cep-small-1009.11.1-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1007.13.0.zip
/resources/kubernetes-images/cep-small-1006.0.10.zip
/resources/kubernetes-images/cep-small-1006.6.38-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1009.3.0.zip
/resources/kubernetes-images/cep-small-1009.9.1-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1006.6.6.zip
/resources/kubernetes-images/cep-small-1005.7.17.zip
/resources/kubernetes-images/cep-small-1009.1.0.zip
/resources/kubernetes-images/cep-small-1006.0.7.zip
/resources/kubernetes-images/cep-small-1006.6.26.zip
/resources/kubernetes-images/cep-small-1007.0.36.zip
/resources/kubernetes-images/cep-small-1009.0.13-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1006.6.2.zip
/resources/kubernetes-images/cep-small-1007.0.23.zip
/resources/kubernetes-images/cep-small-1009.11.0.zip
/resources/kubernetes-images/cep-small-1007.9.0.zip
/resources/kubernetes-images/cep-small-1006.6.31.zip
/resources/kubernetes-images/cep-small-1006.6.4.zip
/resources/kubernetes-images/cep-small-1005.7.14.zip
/resources/kubernetes-images/cep-small-1006.0.20.zip
/resources/kubernetes-images/cep-small-1006.6.37-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1010.0.1.zip
/resources/kubernetes-images/cep-small-1007.0.35.zip
/resources/kubernetes-images/cep-small-1006.0.6.zip
/resources/kubernetes-images/cep-small-1006.6.30.zip
/resources/kubernetes-images/cep-small-1009.0.3.zip
/resources/kubernetes-images/cep-small-1009.5.0.zip
/resources/kubernetes-images/cep-small-1005.7.19.zip
/resources/kubernetes-images/cep-small-1006.0.29.zip
/resources/kubernetes-images/cep-small-1009.6.0.zip
/resources/kubernetes-images/cep-small-9.20.11.zip
/resources/kubernetes-images/cep-small-1006.6.3.zip
/resources/kubernetes-images/cep-small-1007.19.0.zip
/resources/kubernetes-images/cep-small-1006.6.8.zip
/resources/kubernetes-images/cep-small-1009.12.0-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1006.6.36.zip
/resources/kubernetes-images/cep-small-1009.7.0.zip
/resources/kubernetes-images/cep-small-1006.6.32.zip
/resources/kubernetes-images/cep-small-9.20.20.zip
/resources/kubernetes-images/cep-small-1007.0.21.zip
/resources/kubernetes-images/cep-small-1010.3.0-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1009.0.10.zip
/resources/kubernetes-images/cep-small-1006.0.27.zip
/resources/kubernetes-images/cep-small-1009.4.0.zip
/resources/kubernetes-images/cep-small-1009.13.0-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1009.0.11.zip
/resources/kubernetes-images/cep-small-1007.0.39.zip
/resources/kubernetes-images/cep-small-1009.0.5.zip
/resources/kubernetes-images/cep-small-1007.16.0.zip
/resources/kubernetes-images/cep-small-1009.13.0.zip
/resources/kubernetes-images/cep-small-1007.0.38.zip
/resources/kubernetes-images/cep-small-1009.0.1.zip
/resources/kubernetes-images/cep-small-1006.0.3.zip
/resources/kubernetes-images/cep-small-1006.0.11.zip
/resources/kubernetes-images/cep-small-1006.0.14.zip
/resources/kubernetes-images/cep-small-1006.0.30-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1010.2.0.zip
/resources/kubernetes-images/cep-small-1004.6.23.zip
/resources/kubernetes-images/cep-small-1009.0.8-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1007.0.37.zip
/resources/kubernetes-images/cep-small-1006.0.2.zip
/resources/kubernetes-images/cep-small-1006.0.26.zip
/resources/kubernetes-images/cep-small-1006.6.0.zip
/resources/kubernetes-images/cep-small-1007.0.28.zip
/resources/kubernetes-images/cep-small-9.16.15.zip
/resources/kubernetes-images/cep-small-1009.0.12-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1006.6.29.zip
/resources/kubernetes-images/cep-small-1010.0.2-SNAPSHOT.zip
/resources/kubernetes-images/cep-small-1006.6.23.zip
/resources/kubernetes-images/cep-small-1005.0.21.zip
/resources/kubernetes-images/cep-small-1006.6.24.zip
/resources/kubernetes-images/cep-small-1007.17.0.zip
/resources/kubernetes-images/cep-small-1007.15.0.zip

    """.trimIndent()
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .sorted()
        .map { it.substring("/resources/kubernetes-images".length) }

    api.rest().tenant().currentTenant().get().retry().block()

    val applicationApi = api.rest().application()
    val apama = applicationApi.ensureApplication("cep-small")
        .block()

    val tmp = Paths.get("/tmp/cep/")
    Files.createDirectories(tmp)

    val last = Flux.fromIterable(imagesToDownload)
        .log()
        .concatMap { image ->
            images.downloadFile(tmp, image)
        }.log()
        .map { readFile(it) }
        .index()
        .log()
        .concatMap {
            applicationApi.upload(apama.id!!, it.t2).retry(2)
        }
        .blockLast()


}

private fun WebClient.downloadFile(
    tmp: Path,
    image: String,
): Mono<Path> {
    val imageFile = tmp.resolve(image.substring(1))
    return if (Files.exists(imageFile)) {
        Mono.just(imageFile)
    } else {
        DataBufferUtils.write(
            this.get()
                .uri(image)
                .retrieve()
                .bodyToFlux(DataBuffer::class.java),
            imageFile,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE_NEW
        ).then(Mono.just(imageFile))
    }
}

private fun readFile(imageFile: Path) = DataBufferUtils.read(
    imageFile,
    DefaultDataBufferFactory.sharedInstance,
    8 * 1024,
    StandardOpenOption.READ
)

