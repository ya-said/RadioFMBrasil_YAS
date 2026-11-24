package br.radio.fm.player

import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import br.radio.fm.MainActivity
import br.radio.fm.R
import br.radio.fm.RadioApp

/**
 * Serviço de reprodução em primeiro plano. Dono do único ExoPlayer do app,
 * expõe MediaSession (notificação, Bluetooth, tela de bloqueio) e reconecta quando a transmissão cai.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())
    private var retries = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val retryRunnable = Runnable {
        if (player.playWhenReady && player.mediaItemCount > 0) {
            player.seekToDefaultPosition()
            player.prepare()
        }
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                retries = 0
                publishStatus(PlaybackContract.STATUS_OK)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                handler.removeCallbacks(retryRunnable)
                retries = 0
                publishStatus(PlaybackContract.STATUS_OK)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val unrecoverable = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
            if (unrecoverable) {
                handler.removeCallbacks(retryRunnable)
                publishStatus(PlaybackContract.STATUS_FAILED)
            } else {
                scheduleRetry()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(PlaybackContract.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http)))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // gerencia o foco de áudio (pausa em ligações, etc.)
            )
            .setHandleAudioBecomingNoisy(true) // pausa ao desconectar o fone
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.addListener(listener)

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, SkipPlayer(player)).setSessionActivity(openApp).build()

        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.notification_channel)
            .build()
        provider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(provider)

        registerNetworkCallback()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    // Não chama super: deslizar o app da lista de recentes NÃO interrompe a rádio.
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        networkCallback?.let {
            try {
                getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it)
            } catch (e: Exception) {
            }
        }
        networkCallback = null
        session?.release()
        session = null
        player.release()
        super.onDestroy()
    }

    private fun skip(delta: Int) {
        (application as RadioApp).container.player.skip(delta)
    }

    /** Faz a notificação, a tela de bloqueio e os fones Bluetooth mostrarem anterior/próxima. */
    private inner class SkipPlayer(base: Player) : ForwardingPlayer(base) {
        private val skipCommands = intArrayOf(
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        )

        override fun getAvailableCommands(): Player.Commands =
            super.getAvailableCommands().buildUpon().addAll(*skipCommands).build()

        override fun isCommandAvailable(command: Int): Boolean =
            command in skipCommands || super.isCommandAvailable(command)

        override fun hasNextMediaItem(): Boolean = true
        override fun hasPreviousMediaItem(): Boolean = true
        override fun seekToNext() = skip(1)
        override fun seekToNextMediaItem() = skip(1)
        override fun seekToPrevious() = skip(-1)
        override fun seekToPreviousMediaItem() = skip(-1)
    }

    private fun scheduleRetry() {
        handler.removeCallbacks(retryRunnable)
        if (!player.playWhenReady) return
        if (retries >= PlaybackContract.MAX_RETRIES) {
            publishStatus(PlaybackContract.STATUS_FAILED)
            return
        }
        retries++
        publishStatus(PlaybackContract.STATUS_RECONNECTING)
        handler.postDelayed(retryRunnable, (retries * 2_000L).coerceAtMost(10_000L))
    }

    private fun publishStatus(status: String) {
        session?.setSessionExtras(Bundle().apply { putString(PlaybackContract.KEY_STATUS, status) })
    }

    /** Quando a internet volta e a rádio estava parada por falha, tenta de novo na hora. */
    private fun registerNetworkCallback() {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.post {
                    if (player.playWhenReady && player.mediaItemCount > 0 &&
                        player.playbackState == Player.STATE_IDLE
                    ) {
                        retries = 0
                        handler.removeCallbacks(retryRunnable)
                        retryRunnable.run()
                    }
                }
            }
        }
        try {
            manager.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (e: Exception) {
        }
    }
}
