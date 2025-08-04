package com.pcloudai.backend.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateUserSettingsRequest(
    @JsonProperty("theme")
    val theme: String? = null, // "light", "dark", or "system"

    @JsonProperty("notifications")
    val notifications: NotificationSettings? = null
) {
    data class NotificationSettings(
        @JsonProperty("email")
        val email: Boolean? = null,

        @JsonProperty("push")
        val push: Boolean? = null,

        @JsonProperty("weeklyReport")
        val weeklyReport: Boolean? = null
    )
}
