package io.c8y.api.management.cep

import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.io.InputStream
import java.net.URI

class CepApi(
    private val client: WebClient

) {

    fun refresh(): Mono<Void> {


        return client.get()
            .uri { uri -> uri.path("service/cep/cep/microservice/refreshSubscriptions").build() }
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) { r ->
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException(it)
                    }

            }
            .onStatus(HttpStatus::is5xxServerError) { r ->
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException(it)
                    }

            }
            .bodyToMono(Void::class.java)

    }

//    fun realtime(): Realtime {
//
//        return RealtimeWebsocketFlux()
//    }

    fun upload(just: Mono<InputStream>): Mono<Void> {


        return client.post()
            .uri { uri -> uri.path("service/cep/cep/modules").build() }
            .contentType(MediaType.TEXT_PLAIN)
            .body(
                BodyInserters.fromPublisher(
                    just.map { InputStreamResource(it) },
                    ParameterizedTypeReference.forType(InputStreamResource::class.java)
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) { r ->
                r.bodyToMono(String::class.java)
                    .log()
                    .map {
                        RuntimeException(it)
                    }

            }
            .onStatus(HttpStatus::is5xxServerError) { r ->
                r.bodyToMono(String::class.java)
                    .log()
                    .map {
                        RuntimeException(it)
                    }

            }
            .bodyToMono(Void::class.java)

    }

}