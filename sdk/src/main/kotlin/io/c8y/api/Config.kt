package io.c8y.api

interface Config {

    fun <T> get(id:String):T
    fun <T> getOrDefault(key: String,orDefault:T ): T
}