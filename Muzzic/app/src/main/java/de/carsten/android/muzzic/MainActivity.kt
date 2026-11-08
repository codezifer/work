package de.carsten.android.muzzic

import android.Manifest
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import de.carsten.android.muzzic.logging.StartupTrace
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.ui.MusicPlayerApp
import de.carsten.android.muzzic.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val logger = logger()
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        StartupTrace.mark("activity.create")
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
        logFirstFrame()
    }

    /**
     * Marks the first drawn frame, i.e. the moment the splash screen hands over to the UI.
     */
    private fun logFirstFrame() {
        window.decorView.viewTreeObserver.addOnDrawListener(
            object : ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    StartupTrace.markOnce("first.frame")
                    // Listener removal is deferred so it does not mutate the observer mid-dispatch.
                    window.decorView.post {
                        window.decorView.viewTreeObserver.removeOnDrawListener(this)
                    }
                }
            },
        )
    }
}
