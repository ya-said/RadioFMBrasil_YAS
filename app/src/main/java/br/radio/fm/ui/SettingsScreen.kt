package br.radio.fm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val uf by vm.preferredUf.collectAsStateWithLifecycle()
    val lastUpdate by vm.lastUpdate.collectAsStateWithLifecycle()
    val size by vm.catalogSize.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        Text("Tema", style = MaterialTheme.typography.titleMedium)
        listOf(0 to "Seguir o sistema", 1 to "Claro", 2 to "Escuro").forEach { (mode, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = themeMode == mode, role = Role.RadioButton) { vm.setThemeMode(mode) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = themeMode == mode, onClick = null)
                Spacer(Modifier.width(8.dp))
                Text(label)
            }
        }

        HorizontalDivider()
        Text("Região preferida", style = MaterialTheme.typography.titleMedium)
        UfDropdown(selected = uf, allLabel = "Nenhuma") { vm.setPreferredUf(it) }

        HorizontalDivider()
        Text("Catálogo", style = MaterialTheme.typography.titleMedium)
        val updated = if (lastUpdate > 0) DateFormat.getDateTimeInstance().format(Date(lastUpdate)) else "nunca"
        Text(
            "$size emissoras · atualizado: $updated",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { vm.refreshCatalog() }, enabled = !loading) {
            Text(if (loading) "Atualizando…" else "Atualizar agora")
        }
        OutlinedButton(onClick = { vm.clearReported() }) { Text("Mostrar emissoras ocultadas") }

        HorizontalDivider()
        Text("Sobre", style = MaterialTheme.typography.titleMedium)
        Text(
            "Rádio Brasil FM não tem anúncios, conta nem rastreamento. " +
                "O catálogo vem do diretório comunitário radio-browser.info. " +
                "Propagandas que fazem parte da transmissão original de cada emissora continuam sendo tocadas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
