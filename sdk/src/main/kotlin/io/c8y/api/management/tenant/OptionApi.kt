package io.c8y.api.management.tenant

import io.c8y.api.support.Page
import io.c8y.api.support.Pageable
import io.c8y.api.support.Paging
import io.c8y.api.support.handleRestError
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


data class Option(
    val category: String,
    val key: String,
    val value: String
)


data class OptionCollection(val options: Iterable<Option>, override val statistics: Page) :
    Pageable<Option> {
    override fun iterator(): Iterator<Option> {
        return options.iterator()
    }
}

class OptionApi(
    private val client: WebClient
) {
    fun save(option: Option): Mono<Option> {
        return client.post()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Mono.just(option))
            .retrieve()
            .handleRestError()
            .bodyToMono(Option::class.java)
    }

    fun list(vararg params: Pair<String, Any>): Flux<Option> {
        return Paging(client, "").list<Option, OptionCollection>(params)
    }

    fun get(category: String, key: String): Mono<Option> {
        return client.get()
            .uri {
                it.path("$category/$key").build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Option::class.java)
    }

}
