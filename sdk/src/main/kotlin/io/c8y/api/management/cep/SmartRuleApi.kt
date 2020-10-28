package io.c8y.api.management.cep

import com.fasterxml.jackson.annotation.JsonProperty
import io.c8y.api.support.Dynamic
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


class SmartRuleApi(
    private val baseUrl: UriBuilderFactory,
    private val client: WebClient
) {
    fun list(vararg params: Pair<String, Any>): Flux<SmartRule> {
        return client.get()
            .uri {
                it.path("/service/smartrule/smartrules").build()
            }
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) { r ->
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException(it)
                    }

            }
            .onStatus(HttpStatus::is5xxServerError) { r ->
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException(it)
                    }

            }
            .bodyToMono(SmartRuleCollection::class.java)
            .flatMapIterable {
                it.rules
            }
    }

    fun create(smartrule: SmartRule): Mono<SmartRule> {
        return client.post()
            .uri {
                it.path("/service/smartrule/smartrules").build()
            }
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON)
            .body(Mono.just(smartrule), SmartRule::class.java)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) { r ->
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException(it)
                    }

            }
            .onStatus(HttpStatus::is5xxServerError) { r ->
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException(it)
                    }

            }
            .bodyToMono(SmartRule::class.java)
    }

    fun delete(smartRuleId: String): Mono<Void> {
        return client.delete()
            .uri {
                it.path("/service/smartrule/smartrules/{id}").build(smartRuleId)
            }
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) { r ->
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException(it)
                    }

            }
            .onStatus(HttpStatus::is5xxServerError) { r ->
                r.bodyToMono(String::class.java)
                    .map {
                        RuntimeException(it)
                    }

            }
            .bodyToMono(Void::class.java)
    }
}


data class SmartRule(
    val id: String? = null,
    val name: String,
    val type: String,
    val ruleTemplateName: String,
    val enabled: Boolean,
    val enabledSources: Iterable<String>,
    val config: Any,
    @JsonProperty("c8y_Context")
    val context: Any
) : Dynamic<SmartRule>()

data class SmartRuleCollection(
    val rules: List<SmartRule>
)