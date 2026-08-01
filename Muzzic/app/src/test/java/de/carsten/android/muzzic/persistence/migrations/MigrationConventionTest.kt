package de.carsten.android.muzzic.persistence.migrations

import org.junit.Test

class MigrationConventionTest {

    @Test
    fun `all migrations must follow naming convention VStartToEnd_Description`() {
        val subclasses = BaseMigration::class.sealedSubclasses

        subclasses.forEach { kClass ->
            val className = kClass.simpleName ?: "Unknown"
            val migration = kClass.objectInstance
                ?: error("Migration $className must be an 'object'")

            val regex = Regex("^V(\\d+)To(\\d+)_.*$")
            val match = regex.matchEntire(className)

            assert(match != null) {
                "Migration class name '$className' does not follow convention V<Start>To<End>_Description"
            }

            val startVersion = match!!.groupValues[1].toInt()
            val endVersion = match.groupValues[2].toInt()

            assert(migration.startVersion == startVersion) {
                "Migration $className has startVersion ${migration.startVersion}, but name implies $startVersion"
            }
            assert(migration.endVersion == endVersion) {
                "Migration $className has endVersion ${migration.endVersion}, but name implies $endVersion"
            }
        }
    }
}
