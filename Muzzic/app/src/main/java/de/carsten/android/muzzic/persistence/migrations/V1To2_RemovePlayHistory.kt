package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V1To2_RemovePlayHistory : BaseMigration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS play_history")
    }
}
