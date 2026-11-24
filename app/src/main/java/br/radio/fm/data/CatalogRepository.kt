package br.radio.fm.data

import android.content.Context
import br.radio.fm.domain.Station
import br.radio.fm.player.PlaybackContract
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Catálogo de emissoras brasileiras vindo do Radio Browser (https://api.radio-browser.info),
 * com cache em arquivo para funcionar offline.
 */
class CatalogRepository(context: Context, private val client: OkHttpClient) {
    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "catalog_v1.json")
    private val dtoType = object : TypeToken<List<StationDto>>() {}.type
    private val stationType = object : TypeToken<List<Station>>() {}.type

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations

    private val _lastUpdate = MutableStateFlow(0L)
    val lastUpdate: StateFlow<Long> = _lastUpdate

    suspend fun loadCache(): Unit = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext
        try {
            val list: List<Station>? = cacheFile.reader().use { gson.fromJson(it, stationType) }
            _stations.value = list.orEmpty().filter { StreamValidator.isValid(it.url) }
            _lastUpdate.value = cacheFile.lastModified()
        } catch (e: Exception) {
            cacheFile.delete()
        }
    }

    /** Baixa o catálogo atualizado. Tenta cada servidor espelho até um responder. */
    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (host in HOSTS) {
            try {
                val url = "https://$host/json/stations/search".toHttpUrl().newBuilder()
                    .addQueryParameter("countrycode", "BR")
                    .addQueryParameter("hidebroken", "true")
                    .addQueryParameter("order", "clickcount")
                    .addQueryParameter("reverse", "true")
                    .addQueryParameter("limit", "10000")
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", PlaybackContract.USER_AGENT)
                    .build()
                val list = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body ?: throw IOException("Resposta vazia")
                    val dtos: List<StationDto>? = body.charStream().use { gson.fromJson(it, dtoType) }
                    dtos.orEmpty().mapNotNull { it.toStation() }.distinctBy { it.id }
                }
                if (list.isEmpty()) throw IOException("Catálogo vazio")
                val tmp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
                tmp.writeText(gson.toJson(list))
                if (!tmp.renameTo(cacheFile)) {
                    cacheFile.writeText(tmp.readText())
                    tmp.delete()
                }
                _stations.value = list
                _lastUpdate.value = System.currentTimeMillis()
                return@withContext Result.success(list.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: IOException("Falha ao carregar o catálogo"))
    }

    companion object {
        // all.api resolve para os servidores ativos; os demais são espelhos de reserva.
        private val HOSTS = listOf(
            "all.api.radio-browser.info",
            "de1.api.radio-browser.info",
            "fi1.api.radio-browser.info",
            "at1.api.radio-browser.info",
        )
    }
}
