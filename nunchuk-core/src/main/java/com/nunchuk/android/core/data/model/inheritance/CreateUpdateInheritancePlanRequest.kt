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
        val timezone: String? = null,
        @SerializedName("distribution_method")
        val distributionMethod: String? = null,
        @SerializedName("beneficiary_mode")
        val beneficiaryMode: String? = null,
        @SerializedName("buffer_apply_on")
        val bufferApplyOn: String? = null,
        @SerializedName("release_method")
        val releaseMethod: String? = null,
        @SerializedName("fallback_policy")
        val fallbackPolicy: InheritanceFallbackPolicyRequest? = null,
        @SerializedName("stages")
        val stages: List<InheritanceStageRequest>? = null,
        @SerializedName("beneficiaries")
        val beneficiaries: List<InheritanceBeneficiaryRequest>? = null,
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

data class InheritanceFallbackPolicyRequest(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("inactivity_interval")
    val inactivityInterval: String? = null,
    @SerializedName("inactivity_interval_count")
    val inactivityIntervalCount: Int? = null,
    @SerializedName("fallback_time_millis")
    val fallbackTimeMillis: Long? = null,
)

data class InheritanceStageRequest(
    @SerializedName("amount_per_release_percentage")
    val amountPerReleasePercentage: Int? = null,
    @SerializedName("repeat_interval")
    val repeatInterval: String? = null,
    @SerializedName("repeat_interval_count")
    val repeatIntervalCount: Int? = null,
    @SerializedName("total_stage_allocation_percentage")
    val totalStageAllocationPercentage: Int? = null,
    @SerializedName("first_withdrawal_time_millis")
    val firstWithdrawalTimeMillis: Long? = null,
    @SerializedName("expanded_installments")
    val expandedInstallments: List<InheritanceExpandedInstallmentRequest>? = null,
)

data class InheritanceExpandedInstallmentRequest(
    @SerializedName("index")
    val index: Int? = null,
    @SerializedName("withdrawal_time_millis")
    val withdrawalTimeMillis: Long? = null,
    @SerializedName("allocation_percentage")
    val allocationPercentage: Int? = null,
)

data class InheritanceBeneficiaryRequest(
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("asset_percentage")
    val assetPercentage: Int? = null,
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("buffer_period_id")
    val bufferPeriodId: String? = null,
    @SerializedName("buffer_apply_on")
    val bufferApplyOn: String? = null,
    @SerializedName("stages")
    val stages: List<InheritanceStageRequest>? = null,
)
