package br.radio.fm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    MiniPlayer(
                        state = playerState,
                        onToggle = { vm.player.toggle() },
                        onPrevious = { vm.player.skip(-1) },
                        onNext = { vm.player.skip(1) },
                        onOpen = { showPlayer = true },
                    )
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0, onClick = { tab = 0 },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text("Início") },
                        )
                        NavigationBarItem(
                            selected = tab == 1, onClick = { tab = 1 },
                            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            label = { Text("Buscar") },
                        )
                        NavigationBarItem(
                            selected = tab == 2, onClick = { tab = 2 },
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                            label = { Text("Favoritas") },
                        )
                        NavigationBarItem(
                            selected = tab == 3, onClick = { tab = 3 },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text("Ajustes") },
                        )
                    }
                }
            },
        ) { inner ->
            Box(Modifier.padding(inner)) {
                when (tab) {
                    0 -> HomeScreen(vm) { category ->
                        vm.setCategory(category)
                        tab = 1
                    }
                    1 -> SearchScreen(vm)
                    2 -> FavoritesScreen(vm)
                    else -> SettingsScreen(vm)
                }
            }
        }

        if (showPlayer && playerState.station != null) {
            BackHandler { showPlayer = false }
            FullPlayer(vm) { showPlayer = false }
        }
    }
}
