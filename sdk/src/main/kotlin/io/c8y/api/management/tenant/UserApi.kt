package io.c8y.api.management.tenant

import io.c8y.api.support.Dynamic
import io.c8y.api.support.handleRestError
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class UserApi(private val client: WebClient, val tenant: Mono<CurrentTenant>) {

    fun currentUser(): CurrentUserApi {
        return CurrentUserApi(client)
    }
    fun create(user: NewUser): Mono<User> {
        return tenant.flatMap { currentTenant ->
            client.post()
                .uri { it.path("user/{tenant}/users").build(currentTenant.name!!) }
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .body(Mono.just(user), User::class.java)
                .retrieve()
                .handleRestError()
                .bodyToMono(User::class.java)
        }
    }
    fun get(id: String): Mono<User> {

        return tenant.flatMap { currentTenant ->
            client.get()
                .uri { it.path("user/{tenant}/users/{id}").build(currentTenant.name!!, id) }
                .accept(APPLICATION_JSON)
                .retrieve()
                .handleRestError()
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
                .handleRestError()
                .bodyToMono(User::class.java)
        }
    }

}

data class NewUser(
     val userName: String = "admin",
     val password: String = "q1w2e3r4Q!W@E#R$",
     val email: String? = null,
    val twoFactorAuthenticationEnabled: Boolean? = null,
    val phone: String? = null
) : Dynamic<CurrentUser>() {

}
data class User(
    val id: String? = null,
    val twoFactorAuthenticationEnabled: Boolean? = null,
    val phone: String? = null
) : Dynamic<CurrentUser>() {

}

