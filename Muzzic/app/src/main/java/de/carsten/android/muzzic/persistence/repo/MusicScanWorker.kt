package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MusicScanWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    private val repository: MusicRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            repository.performLibraryScan()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
