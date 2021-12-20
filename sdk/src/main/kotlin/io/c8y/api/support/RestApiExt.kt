package io.c8y.api.support

import io.c8y.api.RestError
import io.c8y.api.RestException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.lang.RuntimeException

fun WebClient.ResponseSpec.handleRestError(): WebClient.ResponseSpec {
    return this.onStatus(HttpStatus::is4xxClientError) { r ->
        if (r.headers().asHttpHeaders().contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            r.bodyToMono(RestError::class.java)
                .map {
                    RestException(it, r.statusCode())
                }
        }else{
            r.bodyToMono(String::class.java)
                .map {
                    RuntimeException( "${r.statusCode()} - $it" )
                }
        }
    }
        .onStatus(HttpStatus::is5xxServerError) { r ->
            if (r.headers().asHttpHeaders().contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                r.bodyToMono(RestError::class.java)
                    .map {
                        RestException(it, r.statusCode())
                    }
            }else{
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException( "${r.statusCode()} - $it" )
                    }
            }

        }
}