package de.carsten.android.muzzic.persistence.migrations

import androidx.room.migration.Migration

/**
 * Base class for all database migrations in Muzzic.
 *
 * All migrations MUST inherit from this sealed class to be automatically
 * discovered and validated by [Migrations].
 *
 * @param startVersion The starting database version.
 * @param endVersion The target database version.
 */
sealed class BaseMigration(startVersion: Int, endVersion: Int) : Migration(startVersion, endVersion)
