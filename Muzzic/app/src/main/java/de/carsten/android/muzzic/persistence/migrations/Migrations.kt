package de.carsten.android.muzzic.persistence.migrations

import androidx.room.migration.Migration

/**
 * Registry and utility for Room database migrations.
 *
 * Contains all migrations in an explicit, ordered list. The version chain is
 * validated at runtime to guarantee that it is gap-less: it starts at version 1
 * and reaches the current [VERSION]. While [VERSION] is 1 the list is empty,
 * because the exported baseline schema (1.json) already represents the current
 * entities. The naming convention (V&lt;Start&gt;To&lt;End&gt;_&lt;Description&gt;) is enforced
 * by the unit test
 * [de.carsten.android.muzzic.persistence.migrations.MigrationConventionTest].
 */
object Migrations {
    // Current DB migration version
    const val VERSION = 1

    private val allMigrations: List<Migration> = emptyList<Migration>()
        .also { validateVersionChain(it) }

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
     * An empty list is valid as long as [VERSION] is 1, since the baseline schema
     * needs no migration to be reached.
     *
     * @param migrations The explicit list of registered migrations.
     * @throws IllegalStateException If the chain is incomplete or inconsistent.
     */
    private fun validateVersionChain(migrations: List<Migration>) {
        if (migrations.isEmpty()) {
            check(VERSION == 1) {
                "Migration chain is empty, but VERSION=$VERSION requires migrations from 1 to $VERSION"
            }
            return
        }

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
