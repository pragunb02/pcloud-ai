package com.pcloudai.backend.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.pcloudai.backend.core.domain.UserSettings

data class UserSettingsResponse(
    @JsonProperty("theme")
    val theme: String, // "light", "dark", or "system"

    @JsonProperty("notifications")
    val notifications: NotificationSettings
) {
    data class NotificationSettings(
        @JsonProperty("email")
        val email: Boolean,

        @JsonProperty("push")
        val push: Boolean,

        @JsonProperty("weeklyReport")
        val weeklyReport: Boolean
    )

    companion object {
        fun fromUserSettings(settings: UserSettings): UserSettingsResponse {
            return UserSettingsResponse(
                theme = settings.theme,
                notifications = NotificationSettings(
                    email = settings.emailNotifications,
                    push = settings.pushNotifications,
                    weeklyReport = settings.weeklyReportNotifications
                )
            )
        }
    }
}
