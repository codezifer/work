package de.carsten.android.muzzic

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.ui.MusicPlayerApp
import de.carsten.android.muzzic.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val logger = logger()
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        logger.info("### Starting Muzzic MainActivity (${BuildConfig.VERSION_NAME}) ###")

        requestPermissions(
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
            ),
            1,
        )

        setContent {
            AppTheme {
                MusicPlayerApp()
            }
        }
    }
}
