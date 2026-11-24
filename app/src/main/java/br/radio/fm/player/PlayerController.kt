package br.radio.fm.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.radio.fm.data.CatalogRepository
import br.radio.fm.data.PrefsRepository
import br.radio.fm.domain.QueueNavigator
import br.radio.fm.domain.Station
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Ponte entre a interface e o PlaybackService (via MediaController). Vive enquanto o processo viver. */
class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val catalog: CatalogRepository,
    private val prefs: PrefsRepository,
) {
    enum class Status { OK, RECONNECTING, FAILED }

    data class State(
        val station: Station? = null,
        val isPlaying: Boolean = false,
        val playWhenReady: Boolean = false,
        val isBuffering: Boolean = false,
        val status: Status = Status.OK,
        val nowPlaying: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingStation: Station? = null
    private var appVisible = false
    private var queue: List<Station> = emptyList()

    val sleepTimer = SleepTimer(scope) { stopPlayback() }
    val timerRemainingMs: StateFlow<Long?> = sleepTimer.remainingMs

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            sync(controller)
        }

        override fun onDisconnected(controller: MediaController) {
            this@PlayerController.controller = null
            future = null
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            controller?.let { sync(it) }
        }
    }

    /** Chamado pela Activity: conecta ao serviço quando visível e solta a conexão se nada estiver tocando. */
    fun onAppVisibilityChanged(visible: Boolean) {
        appVisible = visible
        if (visible) connect() else releaseIfIdle()
    }

    private fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).setListener(controllerListener).buildAsync()
        future = f
        f.addListener({
            try {
                val c = f.get()
                controller = c
                c.addListener(playerListener)
                pendingStation?.let {
                    pendingStation = null
                    startPlayback(c, it)
                }
                sync(c)
            } catch (e: Exception) {
                future = null
                controller = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun releaseIfIdle() {
        if (appVisible) return
        val c = controller
        if (c != null && (c.isPlaying || c.playWhenReady)) return
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
    }

    private fun sync(c: MediaController) {
        val item = c.currentMediaItem
        var station = _state.value.station
        if (item != null && station?.id != item.mediaId) {
            station = catalog.stations.value.firstOrNull { it.id == item.mediaId } ?: Station(
                id = item.mediaId,
                name = item.mediaMetadata.title?.toString() ?: "Rádio",
                url = item.localConfiguration?.uri?.toString().orEmpty(),
            )
        }
        val title = c.mediaMetadata.title?.toString()
        val nowPlaying = title?.takeIf { it.isNotBlank() && it != station?.name }
        val status = when (c.sessionExtras.getString(PlaybackContract.KEY_STATUS)) {
            PlaybackContract.STATUS_RECONNECTING -> Status.RECONNECTING
            PlaybackContract.STATUS_FAILED -> Status.FAILED
            else -> Status.OK
        }
        _state.value = State(
            station = station,
            isPlaying = c.isPlaying,
            playWhenReady = c.playWhenReady,
            isBuffering = c.playbackState == Player.STATE_BUFFERING && c.playWhenReady,
            status = status,
            nowPlaying = nowPlaying,
        )
        if (!appVisible && !c.isPlaying && !c.playWhenReady) mainHandler.post { releaseIfIdle() }
    }

    /** [newQueue] é a lista de onde a rádio foi escolhida; "anterior/próxima" andam por ela. */
    fun play(station: Station, newQueue: List<Station>? = null) {
        if (newQueue != null && newQueue.any { it.id == station.id }) {
            queue = newQueue
        } else if (queue.none { it.id == station.id }) {
            queue = listOf(station)
        }
        _state.value = _state.value.copy(
            station = station, playWhenReady = true, isBuffering = true,
            status = Status.OK, nowPlaying = null,
        )
        scope.launch { prefs.addHistory(station) }
        val c = controller
        if (c != null) {
            startPlayback(c, station)
        } else {
            pendingStation = station
            connect()
        }
    }

    private fun startPlayback(c: MediaController, s: Station) {
        val metadata = MediaMetadata.Builder()
            .setTitle(s.name)
            .setArtist(s.state.ifBlank { "Rádio Brasil FM" })
            .setIsPlayable(true)
            .apply { if (s.favicon.isNotBlank()) setArtworkUri(Uri.parse(s.favicon)) }
            .build()
        val item = MediaItem.Builder()
            .setMediaId(s.id)
            .setUri(s.url)
            .setMediaMetadata(metadata)
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    /** Pausa se estiver tocando; senão (re)inicia a transmissão ao vivo do começo. */
    fun toggle() {
        val station = _state.value.station ?: return
        val c = controller
        if (c != null && (c.isPlaying || c.playWhenReady)) c.pause() else play(station)
    }

    /**
     * delta = +1 próxima rádio, -1 rádio anterior. Anda pela lista de onde a rádio foi escolhida;
     * se não houver lista, usa as favoritas e, por último, o catálogo inteiro.
     */
    fun skip(delta: Int) {
        val currentId = _state.value.station?.id ?: return
        scope.launch {
            var list = queue
            var next = QueueNavigator.neighbor(list, currentId, delta)
            if (next == null) {
                list = prefs.favorites.first()
                next = QueueNavigator.neighbor(list, currentId, delta)
            }
            if (next == null) {
                list = catalog.stations.value
                next = QueueNavigator.neighbor(list, currentId, delta)
            }
            if (next != null) play(next, list)
        }
    }

    fun stopPlayback() {
        controller?.let {
            it.stop()
            it.clearMediaItems()
        }
        _state.value = _state.value.copy(isPlaying = false, playWhenReady = false, isBuffering = false)
    }
}
