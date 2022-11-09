package io.c8y.api.management.tenant

import io.c8y.api.support.Dynamic
import io.c8y.api.support.handleRestError
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

data class CurrentUser(
    val name: String?
) : Dynamic<CurrentUser>()

class CurrentUserApi(private val client: WebClient) {
    fun get(): Mono<CurrentUser> {
        return client.get()
            .uri { uri -> uri.path("user/currentUser").build() }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(CurrentUser::class.java)
    }
}