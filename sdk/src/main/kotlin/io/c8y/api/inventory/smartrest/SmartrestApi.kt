package io.c8y.api.inventory.smartrest

import io.c8y.api.support.handleRestError
import io.c8y.api.support.loggerFor
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.lang.StringBuilder

typealias SmartrestRow = String
typealias SmartrestBatch = List<List<String>>

class SmartrestApi(private val client: WebClient, private val path: String) {
    private val log = loggerFor<SmartrestApi>()
    fun send(templateId: String, vararg rows: SmartrestRow): Mono<String> {

        val body = rows.joinTo(StringBuilder(), "\n")
        log.debug("Sending smartrest request {} - X-Id: {}, Body: {}",path,templateId,body)
        return client.post()
            .uri { uri -> uri.path(path).build() }
            .contentType(MediaType.TEXT_PLAIN)
            .header("X-Id", templateId)
            .body(Mono.just(body), String::class.java)
            .accept(MediaType.TEXT_PLAIN)

            .retrieve()
            .handleRestError()
            .bodyToMono(String::class.java)
    }
}

object SmartREST {
    fun row(vararg line: String): SmartrestRow {
        return line.joinToString(",") { value ->
            "\"${value}\""
        };
    }

    fun line(value: String): SmartrestRow {
        return value
    }
}