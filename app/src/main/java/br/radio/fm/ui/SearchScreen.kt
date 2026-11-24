package br.radio.fm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.radio.fm.domain.Category

@Composable
fun SearchScreen(vm: MainViewModel) {
    val results by vm.results.collectAsStateWithLifecycle()
    val uf by vm.uf.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val playing by vm.player.state.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf(vm.query.value) }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; vm.setQuery(it) },
            singleLine = true,
            placeholder = { Text("Nome, cidade ou estado") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = ""; vm.setQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UfDropdown(selected = uf, allLabel = "Todos os estados") { vm.setUf(it) }
            if (uf != null || category != null) {
                TextButton(onClick = { vm.setUf(null); vm.setCategory(null) }) { Text("Limpar filtros") }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Category.values().toList()) { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { vm.setCategory(if (category == cat) null else cat) },
                    label = { Text(cat.label) },
                )
            }
        }
        Text(
            "${results.size} emissoras",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { station ->
                StationItem(
                    station = station,
                    isFavorite = station.id in favoriteIds,
                    isCurrent = playing.station?.id == station.id,
                    onPlay = { vm.play(station, results) },
                    onToggleFavorite = { vm.toggleFavorite(station) },
                )
            }
        }
    }
}
