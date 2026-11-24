package br.radio.fm.data

import br.radio.fm.domain.Station
import com.google.gson.annotations.SerializedName

/** Formato devolvido pela API do Radio Browser (campos podem faltar ou vir nulos). */
data class StationDto(
    @SerializedName("stationuuid") val uuid: String?,
    val name: String?,
    @SerializedName("url_resolved") val urlResolved: String?,
    val url: String?,
    val homepage: String?,
    val favicon: String?,
    val tags: String?,
    val state: String?,
    val codec: String?,
    val bitrate: Int?,
    val votes: Int?,
    val clickcount: Int?,
    val lastcheckok: Int?,
) {
    fun toStation(): Station? {
        val id = uuid?.takeIf { it.isNotBlank() } ?: return null
        val title = name?.trim().orEmpty()
        if (title.isEmpty()) return null
        if (lastcheckok == 0) return null
        val stream = (urlResolved?.takeIf { it.isNotBlank() } ?: url)?.trim()
        if (!StreamValidator.isValid(stream)) return null
        return Station(
            id = id,
            name = title,
            url = stream!!,
            homepage = homepage.orEmpty(),
            favicon = favicon?.takeIf { StreamValidator.isValid(it) }.orEmpty(),
            tags = tags.orEmpty(),
            state = state?.trim().orEmpty(),
            codec = codec.orEmpty(),
            bitrate = bitrate ?: 0,
            votes = votes ?: 0,
            clicks = clickcount ?: 0,
        )
    }
}
