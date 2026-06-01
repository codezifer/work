package de.carsten.android.muzzic

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import de.carsten.android.muzzic.persistence.MuzzicDatabase
import de.carsten.android.muzzic.persistence.databaseModule
import de.carsten.android.muzzic.persistence.mock.DatabaseSeeder
import de.carsten.android.muzzic.persistence.repoModule
import de.carsten.android.muzzic.service.serviceModule
import de.carsten.android.muzzic.ui.uiModule
import de.carsten.android.muzzic.viewmodel.viewModelModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MuzzicPlayerApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader = get()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MuzzicPlayerApplication)
            modules(
                databaseModule,
                repoModule,
                serviceModule,
                viewModelModule,
                uiModule,
            )
        }
        if (BuildConfig.SEED_DATABASE) {
            val db = get<MuzzicDatabase>()
            DatabaseSeeder.seed(db, 100)
        }
    }
}
