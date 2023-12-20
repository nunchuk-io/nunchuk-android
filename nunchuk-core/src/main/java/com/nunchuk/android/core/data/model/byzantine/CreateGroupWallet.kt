package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal data class GroupResponse(
    @SerializedName("created_time_millis")
    val createdTimeMillis: Long? = null,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("members")
    val members: List<MemberResponse>? = null,
    @SerializedName("setup_preference")
    val setupPreference: String? = null,
    @SerializedName("wallet_config")
    val walletConfig: WalletConfigDto? = null,
    @SerializedName("is_locked")
    val isLocked: Boolean? = null,
    @SerializedName("slug")
    val slug: String? = null,
)

data class CreateGroupRequest(
    @SerializedName("members")
    val members: List<MemberRequest>,
    @SerializedName("setup_preference")
    val setupPreference: String,
    @SerializedName("wallet_config")
    val walletConfig: WalletConfigRequest
)

data class MemberRequest(
    @SerializedName("email_or_username")
    val emailOrUsername: String,
    @SerializedName("permissions")
    val permissions: List<String>,
    @SerializedName("role")
    val role: String
)

data class WalletConfigRequest(
    @SerializedName("allow_inheritance")
    val allowInheritance: Boolean,
    @SerializedName("m")
    val m: Int,
    @SerializedName("n")
    val n: Int,
    @SerializedName("required_server_key")
    val requiredServerKey: Boolean
)
