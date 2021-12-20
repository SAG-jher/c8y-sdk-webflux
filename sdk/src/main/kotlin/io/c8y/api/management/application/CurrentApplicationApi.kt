package io.c8y.api.management.application

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Mono


class CurrentApplicationApi(
    private val client: WebClient
){

    fun get(): Mono<Application> {
        return client.get()
            .uri { uri -> uri.path("application/currentApplication").build() }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Application::class.java)
            .onErrorMap {
                val ex = it as WebClientResponseException
                RuntimeException("Failure ${ex.message} ${ex.responseBodyAsString}", ex)
            }
    }
    fun subscriptions(): Mono<ApplicationSubscriptions> {
        return client.get()
            .uri { uri -> uri.path("application/currentApplication/subscriptions").build() }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(ApplicationSubscriptions::class.java)
            .onErrorMap {
                val ex = it as WebClientResponseException
                RuntimeException("Failure ${ex.message} ${ex.responseBodyAsString}", ex)
            }
    }
}