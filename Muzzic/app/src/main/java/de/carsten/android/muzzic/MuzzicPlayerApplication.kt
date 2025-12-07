package de.carsten.android.muzzic

import android.app.Application
import de.carsten.android.muzzic.persistence.databaseModule
import de.carsten.android.muzzic.persistence.repoModule
import de.carsten.android.muzzic.ui.uiModule
import de.carsten.android.muzzic.viewmodel.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MuzzicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MuzzicPlayerApplication)
            modules(
                databaseModule,
                repoModule,
                viewModelModule,
                uiModule,
            )
        }
    }
}
