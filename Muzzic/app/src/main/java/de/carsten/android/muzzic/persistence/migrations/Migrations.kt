package de.carsten.android.muzzic.persistence.migrations

import androidx.room.migration.Migration
import kotlin.reflect.full.*

/**
 * Registry and utility for Room database migrations.
 *
 * This class automatically discovers all sub-classes of [BaseMigration] using
 * Kotlin reflection. It also enforces a strict naming convention to ensure
 * migrations are correctly ordered and documented.
 */
object Migrations {
    // Current DB migration version
    const val VERSION = 2

    /**
     * Regex pattern for migration naming convention: V<Start>To<End>_<Description>
     * Example: V1To2_RemovePlayHistory
     */
    private val MIGRATION_NAME_REGEX = Regex("^V(\\d+)To(\\d+)_.*$")

    private val allMigrations: List<Migration> by lazy {
        BaseMigration::class.sealedSubclasses
            .asSequence()
            .mapNotNull { it.objectInstance }
            .onEach { validateNamingConvention(it) }
            .toList()
    }

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
