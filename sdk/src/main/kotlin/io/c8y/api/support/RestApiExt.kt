package io.c8y.api.support

import io.c8y.api.RestError
import io.c8y.api.RestException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.SECONDS
import java.time.temporal.TemporalAccessor


val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.ssZZ")
fun TemporalAccessor.asQueryParam() =
        when (this) {
            is ZonedDateTime -> timestampFormat.format(
                LocalDateTime.ofInstant(
                    this.toInstant(),
                    ZoneOffset.UTC
                ).atZone(ZoneOffset.UTC).truncatedTo(SECONDS)
            )
            is OffsetDateTime -> timestampFormat.format(this.truncatedTo(SECONDS))
            else -> timestampFormat.format(this)
        }

fun WebClient.ResponseSpec.handleRestError(): WebClient.ResponseSpec {
    return this.onStatus(HttpStatus::is4xxClientError) { r ->
        if (r.headers().asHttpHeaders()?.contentType?.isCompatibleWith(MediaType.APPLICATION_JSON) == true) {
            r.bodyToMono(RestError::class.java)
                .map {
                    RestException(it, r.statusCode())
                }
        } else {
            r.bodyToMono(String::class.java)
                .map {
                    RuntimeException("${r.statusCode()} - $it")
                }
        }
    }
        .onStatus(HttpStatus::is5xxServerError) { r ->
            if (r.headers().asHttpHeaders().contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                r.bodyToMono(RestError::class.java)
                    .map {
                        RestException(it, r.statusCode())
                    }
            } else {
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException("${r.statusCode()} - $it")
                    }
            }

        }
}