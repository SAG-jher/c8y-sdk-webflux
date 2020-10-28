package io.c8y.config

import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

object Environment {


    private val config: Map<String, Any> by lazy {
        try {
            Files.newBufferedReader(
                Paths.get(System.getProperty("user.home"), ".config/sdk-config.yaml")
            ).use {
                return@lazy Yaml().load<Map<String, Any>>(it)
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    operator fun <T> get(id: String): T {
        return config.get(id) as T
    }

    fun <T> getOrDefault(id: String, defaultValue: T): T {
        return config.getOrDefault(id, defaultValue as Any) as T
    }

    operator fun contains(id: String): Boolean {
        return config.containsKey(id)
    }
}