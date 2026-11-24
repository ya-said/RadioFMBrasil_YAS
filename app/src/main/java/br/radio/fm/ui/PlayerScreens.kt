package br.radio.fm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.radio.fm.player.PlayerController

fun statusText(s: PlayerController.State): String = when {
    s.status == PlayerController.Status.FAILED -> "Emissora indisponível"
    s.status == PlayerController.Status.RECONNECTING -> "Reconectando…"
    s.isBuffering -> "Carregando…"
    s.nowPlaying != null -> s.nowPlaying
    s.playWhenReady -> "Ao vivo"
    else -> "Pausado"
}

private fun formatTimer(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun MiniPlayer(
    state: PlayerController.State,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
) {
    val station = state.station ?: return
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            if (state.isBuffering || state.status == PlayerController.Status.RECONNECTING) {
                LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationLogo(station, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        station.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        statusText(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.status == PlayerController.Status.FAILED) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Rádio anterior")
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (state.playWhenReady) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.playWhenReady) "Pausar" else "Reproduzir",
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Próxima rádio")
                }
            }
        }
    }
}

@Composable
fun FullPlayer(vm: MainViewModel, onClose: () -> Unit) {
    val state by vm.player.state.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val remaining by vm.player.timerRemainingMs.collectAsStateWithLifecycle()
    var showTimer by remember { mutableStateOf(false) }
    val station = state.station ?: return
    val failed = state.status == PlayerController.Status.FAILED
    val isFavorite = station.id in favoriteIds

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Fechar player")
                }
                Text("Tocando agora", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.weight(1f))
            StationLogo(station, 240.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                station.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = stationSubtitle(station)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                statusText(state),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (failed) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.play(station) }) { Text("Tentar novamente") }
            }
            remaining?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Desliga em ${formatTimer(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { vm.player.skip(-1) },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Rádio anterior",
                        modifier = Modifier.size(40.dp),
                    )
                }
                LargeFloatingActionButton(onClick = { vm.player.toggle() }) {
                    Icon(
                        if (state.playWhenReady) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.playWhenReady) "Pausar" else "Reproduzir",
                        modifier = Modifier.size(44.dp),
                    )
                }
                IconButton(
                    onClick = { vm.player.skip(1) },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Próxima rádio",
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.toggleFavorite(station) }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showTimer = true }) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = "Temporizador para dormir",
                        tint = if (remaining != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.reportProblem(station); onClose() }) {
                Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Informar problema com esta emissora")
            }
        }
    }

    if (showTimer) {
        AlertDialog(
            onDismissRequest = { showTimer = false },
            title = { Text("Temporizador para dormir") },
            text = {
                Column {
                    listOf(15, 30, 45, 60, 90).forEach { minutes ->
                        TextButton(onClick = { vm.player.sleepTimer.start(minutes); showTimer = false }) {
                            Text("$minutes minutos")
                        }
                    }
                    if (remaining != null) {
                        TextButton(onClick = { vm.player.sleepTimer.cancel(); showTimer = false }) {
                            Text("Cancelar temporizador")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTimer = false }) { Text("Fechar") } },
        )
    }
}
