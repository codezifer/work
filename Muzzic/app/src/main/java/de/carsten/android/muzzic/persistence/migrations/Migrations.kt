package de.carsten.android.muzzic.persistence.migrations

import androidx.room.migration.Migration

/**
 * Registry and utility for Room database migrations.
 *
 * Contains all migrations in an explicit, ordered list. It
 * also enforces a strict naming convention to ensure migrations are correctly
 * ordered and documented.
 */
object Migrations {
    // Current DB migration version
    const val VERSION = 4

    /**
     * Regex pattern for migration naming convention: V<Start>To<End>_<Description>
     * Example: V1To2_RemovePlayHistory
     */
    private val MIGRATION_NAME_REGEX = Regex("^V(\\d+)To(\\d+)_.*$")

    private val allMigrations: List<Migration> = listOf(
        V1To2_RemovePlayHistory,
        V2To3_RestorePlayHistory,
        V3To4_AddQueryIndexes,
    ).onEach { validateNamingConvention(it) }

    /**
     * Validates that the migration's class name matches its version parameters.
     *
     * @param migration The migration to validate.
     * @throws IllegalStateException If the naming convention is violated.
     */
    private fun validateNamingConvention(migration: Migration) {
        val className = migration.javaClass.simpleName
        val match = MIGRATION_NAME_REGEX.matchEntire(className)
            ?: error("Migration $className does not follow the naming convention V<Start>To<End>_Description")

        val startVersion = match.groupValues[1].toInt()
        val endVersion = match.groupValues[2].toInt()

        check(migration.startVersion == startVersion) {
            "Migration $className has startVersion ${migration.startVersion}, but name implies $startVersion"
        }
        check(migration.endVersion == endVersion) {
            "Migration $className has endVersion ${migration.endVersion}, but name implies $endVersion"
        }
    }

    /**
     * Supplies all discovered and validated migrations, sorted by start version.
     */
    fun supply(): Array<Migration> = allMigrations
        .sortedBy { it.startVersion }
        .toTypedArray()
}
