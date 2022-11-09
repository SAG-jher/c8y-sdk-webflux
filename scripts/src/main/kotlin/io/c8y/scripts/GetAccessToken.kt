package io.c8y.scripts

import io.c8y.api.BasicCredentials
import io.c8y.config.Platform

fun main() {
    val api = Platform["schindler-prod"]
    when (api.credentials) {
        is BasicCredentials -> {
            println("Provide token:")
            val totp = readLine()!!
            val basic = api.credentials as BasicCredentials
            val token = api.rest().oauth()

                .token(basic.username, basic.password, totp)
                .block()
            println("AccessToken:")
            println(token.accessToken)
        }
    }


}