package br.radio.fm.domain

data class Station(
    val id: String,
    val name: String,
    val url: String,
    val homepage: String = "",
    val favicon: String = "",
    val tags: String = "",
    val state: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val votes: Int = 0,
    val clicks: Int = 0,
)
