package de.carsten.android.muzzic.persistence.entity

import androidx.room.PrimaryKey
import java.util.UUID

abstract class AbstractEntity {
    @PrimaryKey
    open var id: String = UUID.randomUUID().toString()
}
