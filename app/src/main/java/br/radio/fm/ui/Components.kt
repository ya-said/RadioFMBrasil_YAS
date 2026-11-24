package br.radio.fm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.radio.fm.domain.Brazil
import br.radio.fm.domain.Station
import coil.compose.AsyncImage

@Composable
fun StationLogo(station: Station, size: Dp, modifier: Modifier = Modifier) {
    var loaded by remember(station.favicon) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) {
            Icon(
                Icons.Filled.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size / 2),
            )
        }
        if (station.favicon.isNotBlank()) {
            AsyncImage(
                model = station.favicon,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { loaded = true },
            )
        }
    }
}

fun stationSubtitle(station: Station): String {
    val genre = station.tags.split(",").firstOrNull()?.trim().orEmpty()
    return listOf(station.state, genre).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
fun StationItem(
    station: Station,
    isFavorite: Boolean,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationLogo(station, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                station.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = stationSubtitle(station)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
fun StationCard(station: Station, onPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onPlay),
    ) {
        StationLogo(station, 120.dp)
        Spacer(Modifier.size(6.dp))
        Text(
            station.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StationRow(title: String, stations: List<Station>, onPlay: (Station) -> Unit) {
    if (stations.isEmpty()) return
    Column {
        SectionTitle(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(stations, key = { it.id }) { station ->
                StationCard(station) { onPlay(station) }
            }
        }
    }
}

/** Botão com menu para escolher um estado (UF). */
@Composable
fun UfDropdown(selected: String?, allLabel: String, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            val label = Brazil.byCode(selected)?.name ?: allLabel
            Text(label)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = { onSelect(null); open = false },
            )
            Brazil.ufs.forEach { uf ->
                DropdownMenuItem(
                    text = { Text("${uf.name} (${uf.region})") },
                    onClick = { onSelect(uf.code); open = false },
                )
            }
        }
    }
}
