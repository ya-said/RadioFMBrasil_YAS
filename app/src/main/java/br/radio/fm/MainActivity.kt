package br.radio.fm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.radio.fm.ui.AppRoot
import br.radio.fm.ui.MainViewModel
import br.radio.fm.ui.RadioTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            RadioTheme(themeMode) { AppRoot(viewModel) }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as RadioApp).container.player.onAppVisibilityChanged(true)
    }

    override fun onStop() {
        super.onStop()
        (application as RadioApp).container.player.onAppVisibilityChanged(false)
    }
}
