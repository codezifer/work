package de.carsten.android.muzzic.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Generic entity for app-wide settings using a key-value structure.
 * This allows for flexible storage of various settings without modifying the database schema for each new setting.
 *
 * @property settingName The unique identifier for the setting.
 * @property value The string representation of the setting's value.
 * @property type The type of the value (e.g., "STRING", "INT", "BOOLEAN") for proper conversion.
 */
@Entity(tableName = "generic_settings")
data class GenericSetting(
    @PrimaryKey
    val settingName: String,
    val value: String,
    val type: String = "STRING",
) : AbstractEntity()
