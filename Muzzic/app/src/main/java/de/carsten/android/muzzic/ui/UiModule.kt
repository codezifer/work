package de.carsten.android.muzzic.ui

import android.annotation.SuppressLint
import de.carsten.android.muzzic.ui.image.ImageLoader
import org.koin.dsl.module

@SuppressLint("UnsafeOptInUsageError")
val uiModule =
    module {
        single { ImageLoader(get()) }
    }
