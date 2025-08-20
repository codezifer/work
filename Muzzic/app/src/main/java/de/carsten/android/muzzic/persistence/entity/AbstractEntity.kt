package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
abstract class AbstractEntity {
    @PrimaryKey
    val id: String = UUID.randomUUID().toString()
}
