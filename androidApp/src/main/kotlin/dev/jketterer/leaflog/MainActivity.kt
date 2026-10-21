package dev.jketterer.leaflog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import dev.jketterer.leaflog.presentation.ui.navigation.DeepLinkHandler
import dev.jketterer.leaflog.presentation.ui.navigation.NavRoute

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        handleDeepLink(intent)

        setContent {
            App()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val destination = intent?.getStringExtra("destination") ?: return
        val sessionId = intent.getStringExtra("sessionId")
        when {
            // The Quick Timer has no review, so its ready alert opens the Timer too
            sessionId == null -> DeepLinkHandler.setRoute(NavRoute.TimerRoute())
            destination == "timer" -> DeepLinkHandler.setRoute(NavRoute.TimerRoute(sessionId))
            destination == "steep_complete" ->
                DeepLinkHandler.setRoute(NavRoute.SteepCompleteRoute(sessionId))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
