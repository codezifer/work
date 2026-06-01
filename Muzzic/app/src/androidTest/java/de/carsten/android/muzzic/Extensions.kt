package de.carsten.android.muzzic

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Function to copy the asset to a useable file path
 *
 * @param context testing context as [Context]
 * @param assetName file name within the assets as [String]
 * @return absolute file path as [String]
 */
fun getTestFileFromAssets(context: Context, assetName: String): String {
    val testFile = File(context.cacheDir, assetName)
    if (!testFile.exists()) {
        testFile.mkdirs()
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(testFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    return testFile.absolutePath
}
