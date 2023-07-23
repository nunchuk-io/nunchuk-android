package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.KeyPoliciesDto

internal class UpdateGroupKeyPayload(
    @SerializedName("key_id")
    val keyId: String? = null,
    @SerializedName("key_xfp")
    val keyXfp: String? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("new_policies")
    val newPolicies: KeyPoliciesDto? = null,
    @SerializedName("old_policies")
    val oldPolicies: KeyPoliciesDto? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("notify_keyholders")
    val notifyKeyHolders: Boolean? = null
)