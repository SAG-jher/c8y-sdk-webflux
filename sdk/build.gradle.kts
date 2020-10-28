plugins {
    kotlin("jvm") apply(true)
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api("com.fasterxml.jackson.module:jackson-modules-java8:2+")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2+")
    api("org.springframework.boot:spring-boot-starter-webflux:2+")
    api("io.projectreactor:reactor-core:3+")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:1+")
    api("com.spotify:docker-client:8+")
    api("org.slf4j:slf4j-api:1.7+")
    api("com.hivemq:hivemq-mqtt-client:1+")
    api("com.hivemq:hivemq-mqtt-client-reactor:1+")
    api("io.netty:netty-transport-native-epoll:4.1.49.Final:linux-x86_64")
    api("org.java-websocket:Java-WebSocket:1.4.0")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
