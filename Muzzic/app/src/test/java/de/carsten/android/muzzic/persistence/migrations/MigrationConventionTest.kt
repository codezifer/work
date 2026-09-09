package de.carsten.android.muzzic.persistence.migrations

import org.assertj.core.api.Assertions.assertThat
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

            assertThat(match).describedAs("Migration class name '$className' does not follow convention V<Start>To<End>_Description")
                .isNotNull()

            val startVersion = match!!.groupValues[1].toInt()
            val endVersion = match.groupValues[2].toInt()

            assertThat(migration.startVersion).describedAs("Migration $className has startVersion ${migration.startVersion}, but name implies $startVersion")
                .isEqualTo(startVersion)
            assertThat(migration.endVersion).describedAs("Migration $className has endVersion ${migration.endVersion}, but name implies $endVersion")
                .isEqualTo(endVersion)
        }
    }

    @Test
    fun `all migrations must be registered in Migrations`() {
        val registered = Migrations.supply().toSet()
        val discovered = BaseMigration::class.sealedSubclasses.mapNotNull { it.objectInstance }.toSet()

        val missing = discovered - registered
        assertThat(missing).describedAs("Migrations not registered in Migrations.supply()")
            .isEmpty()
    }
}
