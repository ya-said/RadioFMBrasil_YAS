package br.radio.fm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.radio.fm.domain.Station
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "radio_prefs")

/** Favoritos, histórico e configurações, guardados localmente com DataStore. */
class PrefsRepository(private val context: Context) {
    private val gson = Gson()
    private val stationsType = object : TypeToken<List<Station>>() {}.type
    private val idsType = object : TypeToken<List<String>>() {}.type

    private object Keys {
        val FAVORITES = stringPreferencesKey("favorites")
        val HISTORY = stringPreferencesKey("history")
        val THEME = intPreferencesKey("theme") // 0 sistema, 1 claro, 2 escuro
        val UF = stringPreferencesKey("uf")
        val REPORTED = stringPreferencesKey("reported")
    }

    private fun decodeStations(json: String?): List<Station> = try {
        if (json.isNullOrBlank()) emptyList()
        else gson.fromJson<List<Station>>(json, stationsType).orEmpty()
    } catch (e: Exception) {
        emptyList()
    }

    private fun decodeIds(json: String?): List<String> = try {
        if (json.isNullOrBlank()) emptyList()
        else gson.fromJson<List<String>>(json, idsType).orEmpty()
    } catch (e: Exception) {
        emptyList()
    }

    val favorites: Flow<List<Station>> = context.dataStore.data.map { decodeStations(it[Keys.FAVORITES]) }
    val history: Flow<List<Station>> = context.dataStore.data.map { decodeStations(it[Keys.HISTORY]) }
    val themeMode: Flow<Int> = context.dataStore.data.map { it[Keys.THEME] ?: 0 }
    val preferredUf: Flow<String?> = context.dataStore.data.map { it[Keys.UF] }
    val reported: Flow<Set<String>> = context.dataStore.data.map { decodeIds(it[Keys.REPORTED]).toSet() }

    suspend fun toggleFavorite(station: Station) {
        context.dataStore.edit { p ->
            val list = decodeStations(p[Keys.FAVORITES]).toMutableList()
            if (!list.removeAll { it.id == station.id }) list.add(0, station)
            p[Keys.FAVORITES] = gson.toJson(list)
        }
    }

    suspend fun moveFavorite(index: Int, delta: Int) {
        context.dataStore.edit { p ->
            val list = decodeStations(p[Keys.FAVORITES]).toMutableList()
            val target = index + delta
            if (index in list.indices && target in list.indices) {
                val item = list.removeAt(index)
                list.add(target, item)
                p[Keys.FAVORITES] = gson.toJson(list)
            }
        }
    }

    suspend fun addHistory(station: Station) {
        context.dataStore.edit { p ->
            val list = decodeStations(p[Keys.HISTORY]).toMutableList()
            list.removeAll { it.id == station.id }
            list.add(0, station)
            p[Keys.HISTORY] = gson.toJson(list.take(30))
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it.remove(Keys.HISTORY) }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[Keys.THEME] = mode }
    }

    suspend fun setPreferredUf(uf: String?) {
        context.dataStore.edit { p ->
            p.remove(Keys.UF)
            if (uf != null) p[Keys.UF] = uf
        }
    }

    suspend fun report(stationId: String) {
        context.dataStore.edit { p ->
            val ids = decodeIds(p[Keys.REPORTED]).toMutableSet()
            ids.add(stationId)
            p[Keys.REPORTED] = gson.toJson(ids.toList())
        }
    }

    suspend fun clearReported() {
        context.dataStore.edit { it.remove(Keys.REPORTED) }
    }
}
