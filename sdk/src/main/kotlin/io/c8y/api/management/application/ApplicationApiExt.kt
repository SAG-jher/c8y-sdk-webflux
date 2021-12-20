package io.c8y.api.management.application

import reactor.core.publisher.Mono


fun ApplicationApi.ensureApplication(
    name:String
) = this.list("name" to name)
    .take(1)
    .singleOrEmpty()
    .switchIfEmpty(Mono.defer {
        this.create(
            Application(
                name = name,
                type = ApplicationType.MICROSERVICE,
                key = "$name-key"
            )
        )

    })