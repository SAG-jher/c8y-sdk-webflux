package io.c8y.config

import io.c8y.api.BasicCredentials
import io.c8y.api.Config
import io.c8y.api.PlatformApi

object Platform {


    val default: PlatformApi
        get() {
            val defaultEnv = Environment.getOrDefault("default-environment", "local" as Any) as String
            return get(defaultEnv)
        }


    operator fun get(id: String): PlatformApi {
        val envs: Map<String, Map<String, Any>> = Environment["environments"]
        val env: Map<String, Any> = envs[id] ?: error("Can't find environment with id $id")

        require("credentials" in env) { "Can't find credentials for environment with id $id" }
        val credentials = env["credentials"] as Map<String, Any>
        return PlatformApi(
            config = object : Config {
                override fun <T> get(key: String): T {
                    return (env[key] as T)!!
                }
                override fun <T> getOrDefault(key: String,orDefault:T ): T {
                    return (env[key] as T)?:orDefault
                }
            },
            credentials = BasicCredentials(
                username = credentials["username"] as String?
                    ?: error("Can't find username in credentails for environment with id $id"),
                password = credentials["password"] as String?
                    ?: error("Can't find password in credentails for environment with id $id")
            ),
            baseUrl = env["baseUrl"] as String?
                ?: error("Can't find baseUrl for environment with id $id"),
            tenantDomainSupport = env.getOrDefault("tenant-domain-support", true as Any) as Boolean
        )
    }

}

