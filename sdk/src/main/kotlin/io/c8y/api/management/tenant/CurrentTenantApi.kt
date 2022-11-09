package io.c8y.api.management.tenant

import io.c8y.api.support.handleRestError
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class CurrentTenantApi(
    val client: WebClient
) {
    fun get(): Mono<CurrentTenant> {
        return client.get().uri { uri -> uri.path("tenant/currentTenant").build() }
            .retrieve()
            .handleRestError()
            .bodyToMono(CurrentTenant::class.java)
    }

}
