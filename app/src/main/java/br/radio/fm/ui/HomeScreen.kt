package br.radio.fm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.radio.fm.domain.Brazil
import br.radio.fm.domain.Category

@Composable
fun HomeScreen(vm: MainViewModel, onOpenSearch: (Category?) -> Unit) {
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val popular by vm.popular.collectAsStateWithLifecycle()
    val mostListened by vm.mostListened.collectAsStateWithLifecycle()
    val regional by vm.regional.collectAsStateWithLifecycle()
    val uf by vm.preferredUf.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val size by vm.catalogSize.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "Rádio Brasil FM",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onOpenSearch(null) }
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Buscar emissora, cidade ou estado", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (loading) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        "Carregando catálogo…",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (size == 0 && !loading) {
            item {
                Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Nenhuma emissora carregada ainda.", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Verifique sua conexão com a internet e tente novamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        Button(onClick = { vm.refreshCatalog() }) { Text("Tentar novamente") }
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item { StationRow("Suas favoritas", favorites) { vm.play(it, favorites) } }
        }
        item {
            if (uf == null) {
                Column {
                    SectionTitle("Na sua região")
                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        UfDropdown(selected = null, allLabel = "Escolher meu estado") { vm.setPreferredUf(it) }
                    }
                }
            } else {
                StationRow("Na sua região (${Brazil.byCode(uf)?.name ?: uf})", regional) { vm.play(it, regional) }
            }
        }
        item { StationRow("Populares", popular) { vm.play(it, popular) } }
        item { StationRow("Mais ouvidas", mostListened) { vm.play(it, mostListened) } }
        item {
            Column {
                SectionTitle("Categorias")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(Category.values().toList()) { category ->
                        AssistChip(onClick = { onOpenSearch(category) }, label = { Text(category.label) })
                    }
                }
            }
        }
    }
}
