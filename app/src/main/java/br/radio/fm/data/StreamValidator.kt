package br.radio.fm.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object StreamValidator {
    /** Aceita apenas endereços http/https bem formados. */
    fun isValid(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val parsed = url.trim().toHttpUrlOrNull() ?: return false
        return parsed.scheme == "http" || parsed.scheme == "https"
    }
}
