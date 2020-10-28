package io.c8y.api.management.tenant

import io.c8y.api.support.Dynamic
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class UserApi(private val client: WebClient, val tenant: Mono<CurrentTenant>) {

    fun currentUser(): CurrentUserApi {
        return CurrentUserApi(client)
    }

    fun get(id: String): Mono<User> {

        return tenant.flatMap { currentTenant ->
            client.get()
                .uri { it.path("user/{tenant}/users/{id}").build(currentTenant.name!!, id) }
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(User::class.java)
        }

    }

    fun update(user: User): Mono<User> {
        return tenant.flatMap { currentTenant ->
            client.put()
                .uri { it.path("user/{tenant}/users/{id}").build(currentTenant.name!!, user.id!!) }
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(Mono.just(user), User::class.java)
                .retrieve()
                .bodyToMono(User::class.java)
        }
    }

}

data class User(
    val id: String? = null,
    val twoFactorAuthenticationEnabled: Boolean? = null,
    val phone: String? = null
) : Dynamic<CurrentUser>() {

}

