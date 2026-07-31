package de.carsten.android.muzzic.persistence.migrations

import androidx.room.migration.Migration

object Migrations {
    // current DB migration version
    const val VERSION = 1

    private val allMigrations: List<Migration> = listOf()

    fun supply(): Array<Migration> = allMigrations
        .sortedBy { it.javaClass.simpleName }
        .toTypedArray()

}
