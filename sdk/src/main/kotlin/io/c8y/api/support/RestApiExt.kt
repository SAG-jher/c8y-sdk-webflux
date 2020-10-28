package io.c8y.api.support

import io.c8y.api.RestException
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient

fun WebClient.ResponseSpec.handleRestError(): WebClient.ResponseSpec {
    return  this.onStatus(HttpStatus::is4xxClientError) { r ->
        r.bodyToMono(String::class.java)
            .map {
                RestException(it, r.statusCode())
            }

    }
        .onStatus(HttpStatus::is5xxServerError) { r ->
            r.bodyToMono(String::class.java)
                .map {
                    RestException(it, r.statusCode())
                }

        }
}