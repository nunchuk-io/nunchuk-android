package com.nunchuk.android.core.data.model.inheritance

import com.google.gson.annotations.SerializedName

data class CreateUpdateInheritancePlanRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("note")
        val note: String? = null,
        @SerializedName("notification_emails")
        val notificationEmails: List<String>? = null,
        @SerializedName("notify_today")
        val notifyToday: Boolean? = null,
        @SerializedName("activation_time_milis")
        val activationTimeMilis: Long? = null,
        @SerializedName("wallet")
        val walletId: String? = null,
        @SerializedName("buffer_period_id")
        val bufferPeriodId: String? = null,
        @SerializedName("group_id")
        val groupId: String? = null,
        @SerializedName("notification_preferences")
        val notificationPreferences: NotificationPreferencesRequest? = null,
        @SerializedName("timezone")
        val timezone: String
    )
}

data class NotificationPreferencesRequest(
    @SerializedName("email_me_wallet_config")
    val emailMeWalletConfig: Boolean? = null,
    @SerializedName("beneficiary_notifications")
    val beneficiaryNotifications: List<BeneficiaryNotificationRequest>? = null
)

data class BeneficiaryNotificationRequest(
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("notify_timelock_expires")
    val notifyTimelockExpires: Boolean? = null,
    @SerializedName("notify_wallet_changes")
    val notifyWalletChanges: Boolean? = null,
    @SerializedName("include_wallet_config")
    val includeWalletConfig: Boolean? = null
)