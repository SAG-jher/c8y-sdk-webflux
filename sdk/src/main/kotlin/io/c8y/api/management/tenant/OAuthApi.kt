package io.c8y.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.c8y.api.support.handleRestError
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Mono

data class Token(@JsonProperty("access_token") val accessToken: String)
class OAuthApi(val client: WebClient, val baseUrl: UriBuilderFactory) {

    fun token(username: String, password: String, totpCode: String): Mono<Token> {
        return client
            .post()
            .uri { uri -> uri.path("/tenant/oauth/token").build() }
            .body(BodyInserters.fromFormData(LinkedMultiValueMap<String, String>().apply {
                put("grant_type", listOf("PASSWORD"))
                put("username", listOf(username))
                put("password", listOf(password))
                put("tfa_code", listOf(totpCode))
            }))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(String::class.java)
            .map { jacksonObjectMapper().readValue(it, Token::class.java) }
    }
}
