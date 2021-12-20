package io.c8y.api.support

data class Page(val next: String?) {
    fun hasNext(): Boolean {
        return next != null;
    }
    companion object{
        val empty = Page(null);
    }
}