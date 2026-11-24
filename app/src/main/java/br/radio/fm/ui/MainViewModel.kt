package br.radio.fm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.radio.fm.RadioApp
import br.radio.fm.domain.Category
import br.radio.fm.domain.IndexedStation
import br.radio.fm.domain.Station
import br.radio.fm.domain.StationFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as RadioApp).container
    private val catalog = container.catalog
    private val prefs = container.prefs
    val player = container.player

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val lastUpdate: StateFlow<Long> = catalog.lastUpdate

    // ---- filtros da busca ----
    private val _query = MutableStateFlow("")
    private val _uf = MutableStateFlow<String?>(null)
    private val _category = MutableStateFlow<Category?>(null)
    val query: StateFlow<String> = _query.asStateFlow()
    val uf: StateFlow<String?> = _uf.asStateFlow()
    val category: StateFlow<Category?> = _category.asStateFlow()

    // ---- dados persistidos ----
    val favorites = prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history = prefs.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val themeMode = prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val preferredUf = prefs.preferredUf.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val favoriteIds = favorites.map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // ---- catálogo (sem as emissoras que o usuário marcou como com problema) ----
    private val index: StateFlow<List<IndexedStation>> =
        combine(catalog.stations, prefs.reported) { stations, reported ->
            stations.filter { it.id !in reported }.map { IndexedStation(it) }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val catalogSize = index.map { it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val results = combine(index, _query, _uf, _category) { idx, q, uf, cat ->
        StationFilter.apply(idx, q, uf, cat)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val popular = index.map { idx -> idx.map { it.station }.sortedByDescending { it.votes }.take(20) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostListened = index.map { idx -> idx.map { it.station }.sortedByDescending { it.clicks }.take(20) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val regional = combine(index, prefs.preferredUf) { idx, uf ->
        if (uf == null) emptyList<Station>()
        else idx.filter { it.uf == uf }.map { it.station }.sortedByDescending { it.clicks }.take(30)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            catalog.loadCache()
            val stale = System.currentTimeMillis() - catalog.lastUpdate.value > ONE_DAY_MS
            if (catalog.stations.value.isEmpty() || stale) doRefresh(manual = false)
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch { doRefresh(manual = true) }
    }

    private suspend fun doRefresh(manual: Boolean) {
        if (_loading.value) return
        _loading.value = true
        val result = catalog.refresh()
        _loading.value = false
        val hasCache = catalog.stations.value.isNotEmpty()
        when {
            result.isSuccess && manual ->
                messages.tryEmit("Catálogo atualizado: ${result.getOrNull()} emissoras")
            result.isFailure && !hasCache ->
                messages.tryEmit("Não foi possível carregar o catálogo. Verifique sua conexão e tente de novo.")
            result.isFailure ->
                messages.tryEmit("Sem conexão com o catálogo. Usando a lista salva no aparelho.")
        }
    }

    fun setQuery(value: String) { _query.value = value }
    fun setUf(value: String?) { _uf.value = value }
    fun setCategory(value: Category?) { _category.value = value }

    /** [queue] é a lista de onde a rádio foi escolhida (usada pelos botões anterior/próxima). */
    fun play(station: Station, queue: List<Station>? = null) = player.play(station, queue)

    fun toggleFavorite(station: Station) { viewModelScope.launch { prefs.toggleFavorite(station) } }
    fun moveFavorite(index: Int, delta: Int) { viewModelScope.launch { prefs.moveFavorite(index, delta) } }
    fun clearHistory() { viewModelScope.launch { prefs.clearHistory() } }
    fun setThemeMode(mode: Int) { viewModelScope.launch { prefs.setThemeMode(mode) } }
    fun setPreferredUf(uf: String?) { viewModelScope.launch { prefs.setPreferredUf(uf) } }

    fun clearReported() {
        viewModelScope.launch {
            prefs.clearReported()
            messages.emit("Emissoras ocultadas voltaram à lista.")
        }
    }

    /** Oculta a emissora neste aparelho (não há servidor próprio para receber relatórios). */
    fun reportProblem(station: Station) {
        viewModelScope.launch {
            prefs.report(station.id)
            if (player.state.value.station?.id == station.id) player.stopPlayback()
            messages.emit("Emissora ocultada da sua lista. Obrigado por avisar.")
        }
    }

    private companion object {
        const val ONE_DAY_MS = 24L * 60 * 60 * 1000
    }
}
