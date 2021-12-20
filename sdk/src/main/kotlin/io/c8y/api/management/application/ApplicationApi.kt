package io.c8y.api.management.application

import io.c8y.api.support.Paging
import io.c8y.api.support.handleRestError
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ApplicationApi(
    private val baseUrl: UriBuilderFactory,
    private val client: WebClient
) {

    fun list(vararg params: Pair<String, Any>): Flux<Application> {
        return Paging(client, path = "application/applications")
            .list<Application, ApplicationCollection>(params)
    }

    fun get(applicationId: String): Mono<Application> {
        return client.get()
            .uri { uri -> uri.path("application/applications/{id}").build(applicationId) }
            .retrieve()
            .handleRestError()
            .bodyToMono(Application::class.java)

    }

    fun currentApplication(): CurrentApplicationApi {
        return CurrentApplicationApi(client)
    }

    fun bootstrapUser(applicationId: String): Mono<ApplicationUser> {
        return client.get()
            .uri { uri -> uri.path("application/applications/{id}/bootstrapUser").build(applicationId) }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(ApplicationUser::class.java)
    }


    fun subscriptions(applicationId: String): Mono<ApplicationSubscriptions> {
        return client.get()
            .uri { uri -> uri.path("application/applications/{id}/subscriptions").build(applicationId) }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(ApplicationSubscriptions::class.java)

    }

    fun create(application: Application): Mono<Application> {
        return client.post()
            .uri { uri -> uri.path("application/applications").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(application), Application::class.java)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Application::class.java)

    }


    fun upload( id: String, input: Publisher<DataBuffer>): Mono<Void> {
        val bodyBuilder = MultipartBodyBuilder()

        bodyBuilder.asyncPart("file", input, DataBuffer::class.java)

        return client.post()
            .uri { uri -> uri.path("application/applications/{id}/binaries").build(id) }
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(
                BodyInserters.fromMultipartData(
                    bodyBuilder.build()
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .handleRestError()
            .bodyToMono(Void::class.java)
    }

    fun delete(applicationId: String): Mono<Void> {
        return client.delete()
            .uri { uri -> uri.path("application/applications/{id}").build(applicationId) }
            .retrieve()
            .handleRestError()
            .bodyToMono(Void::class.java)

    }


    fun clientFor(service: String): WebClient {
        return client.mutate().baseUrl(baseUrl.builder().path("/service/").path(service).path("/").build().toString())
            .build()
    }

}