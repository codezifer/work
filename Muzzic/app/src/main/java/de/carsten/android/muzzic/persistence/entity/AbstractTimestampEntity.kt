package de.carsten.android.muzzic.persistence.entity

import androidx.room.ColumnInfo
import java.time.Instant

abstract class AbstractTimestampEntity {
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    open var createdAt: Instant? = Instant.now()

    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    open var updatedAt: Instant? = Instant.now()
}
