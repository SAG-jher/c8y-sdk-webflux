package io.c8y.api.management.tenant

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono


data class Option(
    val category: String,
    val key: String,
    val value:String
)

class OptionApi(
    private val client: WebClient
) {
    fun save(option: Option): Mono<Option>{
        return client.post()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Mono.just(option))
            .retrieve()
            .bodyToMono(Option::class.java)
    }

}
