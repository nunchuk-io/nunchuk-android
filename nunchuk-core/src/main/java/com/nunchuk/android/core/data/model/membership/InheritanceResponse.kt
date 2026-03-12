/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.byzantine.DummyTransactionDto

class InheritanceResponse(
    @SerializedName("inheritance")
    val inheritance: InheritanceDto? = null,
    @SerializedName("dummy_transaction")
    val dummyTransaction: DummyTransactionDto? = null
)

class InheritanceDto(
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("notification_emails")
    val notificationEmails: List<String>? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("activation_time_milis")
    val activationTimeMilis: Long? = null,
    @SerializedName("created_time_milis")
    val createdTimeMilis: Long? = null,
    @SerializedName("last_modified_time_milis")
    val lastModifiedTimeMilis: Long? = null,
    @SerializedName("buffer_period")
    val bufferPeriod: PeriodResponse.Data? = null,
    @SerializedName("owner_id")
    val ownerId: String? = null,
    @SerializedName("pending_requests")
    val pendingRequests: List<InheritancePendingRequestResponse>? = null,
    @SerializedName("wallet_type")
    val walletType: String? = null,
    @SerializedName("notification_preferences")
    val notificationPreferences: InheritanceNotificationPreferencesDto? = null,
    @SerializedName("distribution_method")
    val distributionMethod: String? = null,
    @SerializedName("beneficiary_mode")
    val beneficiaryMode: String? = null,
    @SerializedName("buffer_apply_on")
    val bufferApplyOn: String? = null,
    @SerializedName("release_method")
    val releaseMethod: String? = null,
    @SerializedName("fallback_policy")
    val fallbackPolicy: InheritanceFallbackPolicyDto? = null,
    @SerializedName("stages")
    val stages: List<InheritanceStageDto>? = null,
    @SerializedName("beneficiaries")
    val beneficiaries: List<InheritanceBeneficiaryDto>? = null,
    @SerializedName("inheritance_keys")
    val inheritanceKeys: List<InheritanceKeyDto>? = null,
    @SerializedName("timezone")
    val timezone: String? = null,
)

class InheritancePendingRequestResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("membership_id")
    val membershipId: String? = null,
    @SerializedName("dummy_transaction_id")
    val dummyTransactionId: String? = null,
)

class InheritanceNotificationPreferencesDto(
    @SerializedName("email_me_wallet_config")
    val emailMeWalletConfig: Boolean? = null,
    @SerializedName("beneficiary_notifications")
    val beneficiaryNotifications: List<BeneficiaryNotificationDto>? = null,
)

class BeneficiaryNotificationDto(
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("notify_timelock_expires")
    val notifyTimelockExpires: Boolean? = null,
    @SerializedName("notify_wallet_changes")
    val notifyWalletChanges: Boolean? = null,
    @SerializedName("include_wallet_config")
    val includeWalletConfig: Boolean? = null,
)

class InheritanceKeyDto(
    @SerializedName("xfp")
    val xfp: String? = null,
)

class InheritanceFallbackPolicyDto(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("inactivity_interval")
    val inactivityInterval: String? = null,
    @SerializedName("inactivity_interval_count")
    val inactivityIntervalCount: Int? = null,
    @SerializedName("fallback_time_millis")
    val fallbackTimeMillis: Long? = null,
)

class InheritanceStageDto(
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
    val expandedInstallments: List<InheritanceExpandedInstallmentDto>? = null,
)

class InheritanceExpandedInstallmentDto(
    @SerializedName("index")
    val index: Int? = null,
    @SerializedName("withdrawal_time_millis")
    val withdrawalTimeMillis: Long? = null,
    @SerializedName("allocation_percentage")
    val allocationPercentage: Int? = null,
)

class InheritanceBeneficiaryDto(
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
    @SerializedName("buffer_period")
    val bufferPeriod: PeriodResponse.Data? = null,
    @SerializedName("buffer_apply_on")
    val bufferApplyOn: String? = null,
    @SerializedName("stages")
    val stages: List<InheritanceStageDto>? = null,
)
