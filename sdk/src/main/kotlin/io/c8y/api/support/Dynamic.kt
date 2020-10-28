package io.c8y.api.support

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter

open class Dynamic<Derived : Dynamic<Derived>>(private val attrs: MutableMap<String, Any?>? = mutableMapOf()) {
    @JsonAnyGetter
    fun get(): MutableMap<String, Any?>? {
        return attrs
    }

    @JsonAnySetter
    fun set(prop: String, value: Any?): Derived {
        attrs?.put(prop, value)
        return this as Derived
    }

    fun set(frag: Any): Derived {
        attrs?.put(frag.javaClass.name.replace('.','_'), frag)
        return this as Derived
    }
}