package com.adygyes.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for app_settings table row
 * Single key-value pair from Supabase
 */
@Serializable
data class AppSettingDto(
    @SerialName("key") val key: String,
    @SerialName("value") val value: String,
    @SerialName("updated_at") val updatedAt: String? = null
)
