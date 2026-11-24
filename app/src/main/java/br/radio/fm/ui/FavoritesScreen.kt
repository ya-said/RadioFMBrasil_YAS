package br.radio.fm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FavoritesScreen(vm: MainViewModel) {
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val playing by vm.player.state.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 12.dp)) {
        item { SectionTitle("Favoritas") }
        if (favorites.isEmpty()) {
            item {
                Text(
                    "Toque no coração de uma emissora para adicioná-la aqui.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        itemsIndexed(favorites, key = { _, s -> "f${s.id}" }) { index, station ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.play(station, favorites) }
                    .padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationLogo(station, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        station.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (playing.station?.id == station.id) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stationSubtitle(station),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { vm.moveFavorite(index, -1) }, enabled = index > 0) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Mover para cima")
                }
                IconButton(onClick = { vm.moveFavorite(index, 1) }, enabled = index < favorites.lastIndex) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Mover para baixo")
                }
                IconButton(onClick = { vm.toggleFavorite(station) }) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Remover dos favoritos",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) { SectionTitle("Ouvidas recentemente") }
                if (history.isNotEmpty()) {
                    TextButton(onClick = { vm.clearHistory() }) { Text("Limpar") }
                }
            }
        }
        if (history.isEmpty()) {
            item {
                Text(
                    "Nada por aqui ainda.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(history, key = { "h${it.id}" }) { station ->
            StationItem(
                station = station,
                isFavorite = station.id in favoriteIds,
                isCurrent = playing.station?.id == station.id,
                onPlay = { vm.play(station, history) },
                onToggleFavorite = { vm.toggleFavorite(station) },
            )
        }
    }
}
