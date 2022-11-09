import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply(true)
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api("com.fasterxml.jackson.module:jackson-modules-java8:2.13.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("com.google.guava:guava:31.1-jre")
    api("com.hivemq:hivemq-mqtt-client:1.3.0")
    api("com.hivemq:hivemq-mqtt-client-reactor:1.3.0")
    api("io.netty:netty-transport-native-epoll:4.1.49.Final:linux-x86_64")
    api("io.projectreactor:reactor-core:3.4.23")
    api("io.projectreactor.addons:reactor-extra:3.4.8")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.6")
    api("org.springframework.boot:spring-boot-starter-webflux:2.7.4")
    api("org.slf4j:slf4j-api:1.7.36")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
