package de.carsten.android.muzzic.persistence.migrations

import androidx.room.migration.Migration

/**
 * Registry and utility for Room database migrations.
 *
 * Contains all migrations in an explicit, ordered list. The version chain is
 * validated at runtime to guarantee that the list starts at version 1, is gap-less
 * and reaches the current [VERSION]. The naming convention
 * (V&lt;Start&gt;To&lt;End&gt;_&lt;Description&gt;) is enforced by the unit test
 * [de.carsten.android.muzzic.persistence.migrations.MigrationConventionTest].
 */
object Migrations {
    // Current DB migration version
    const val VERSION = 5

    private val allMigrations: List<Migration> = listOf(
        V1To2_RemovePlayHistory,
        V2To3_RestorePlayHistory,
        V3To4_AddQueryIndexes,
        V4To5_CreateSongsEnrichedView,
    ).also { validateVersionChain(it) }

    /**
     * Supplies all migrations, sorted by start version.
     */
    fun supply(): Array<Migration> = allMigrations
        .sortedBy { it.startVersion }
        .toTypedArray()

    /**
     * Validates that the explicit migration list forms a gap-less chain from
     * version 1 up to the current [VERSION].
     *
     * @param migrations The explicit list of registered migrations.
     * @throws IllegalStateException If the chain is incomplete or inconsistent.
     */
    private fun validateVersionChain(migrations: List<Migration>) {
        check(migrations.isNotEmpty()) { "At least one migration must be registered" }

        val first = migrations.minByOrNull { it.startVersion }!!
        check(first.startVersion == 1) {
            "Migration chain must start at version 1, but starts at ${first.startVersion}"
        }

        val sorted = migrations.sortedBy { it.startVersion }
        sorted.zipWithNext().forEach { (current, next) ->
            check(current.endVersion == next.startVersion) {
                "Migration chain has a gap: migration ending at ${current.endVersion} is followed by one starting at ${next.startVersion}"
            }
        }

        val last = sorted.maxByOrNull { it.endVersion }!!
        check(last.endVersion == VERSION) {
            "Migration chain must end at VERSION=$VERSION, but ends at ${last.endVersion}"
        }
    }
}
