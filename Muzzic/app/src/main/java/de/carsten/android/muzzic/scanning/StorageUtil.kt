package de.carsten.android.muzzic.scanning

import android.content.Context
import android.os.storage.StorageManager

/**
 * Utility class for interacting with the device's storage.
 */
class StorageUtil(context: Context) {
    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    /**
     * Get a list of mounted removable storage volumes.
     *
     * @return A list of paths to the mounted removable storage volumes.
     */
    fun getMountedVolumes(): List<String> {
        val volumes = mutableListOf<String>()
        for (volume in storageManager.storageVolumes) {
            if (volume.isRemovable) {
                val path = volume.directory?.path ?: continue
                volumes.add(path)
            }
        }
        return volumes
    }
}
