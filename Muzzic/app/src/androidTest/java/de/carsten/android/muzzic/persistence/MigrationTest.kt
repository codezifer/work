package de.carsten.android.muzzic.persistence

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.carsten.android.muzzic.persistence.migrations.Migrations
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that the exported baseline schema (1.json) still matches the current
 * entities and views.
 *
 * There are no step-by-step migrations while [Migrations.VERSION] is 1. With the
 * first schema change the chain (and its `migrateXToY` tests) is added again.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MuzzicDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun baselineSchemaMatchesEntities() {
        // Builds a database from the exported baseline schema (1.json) …
        helper.createDatabase(testDbName, Migrations.VERSION).close()

        // … then re-opens it through the (empty) migration chain and validates it
        // against the entities compiled into the app.
        val db = helper.runMigrationsAndValidate(testDbName, Migrations.VERSION, true, *Migrations.supply())
        db.close()
    }
}
