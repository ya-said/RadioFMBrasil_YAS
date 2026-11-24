package br.radio.fm

import android.app.Application
import android.content.Context
import br.radio.fm.data.CatalogRepository
import br.radio.fm.data.CatalogWorker
import br.radio.fm.data.PrefsRepository
import br.radio.fm.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Injeção de dependências manual (suficiente para este tamanho de projeto, sem Hilt). */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Somente TLS moderno: chamadas à API do catálogo NUNCA vão por HTTP puro.
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    val prefs = PrefsRepository(context)
    val catalog = CatalogRepository(context, http)
    val player = PlayerController(context, appScope, catalog, prefs)
}

class RadioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CatalogWorker.schedule(this)
    }
}
