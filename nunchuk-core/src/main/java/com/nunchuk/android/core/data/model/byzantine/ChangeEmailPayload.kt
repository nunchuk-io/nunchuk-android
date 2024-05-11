package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.KeyPoliciesDto

class ChangeEmailPayload(
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("new_email")
    val newEmail: String? = null,
    @SerializedName("old_email")
    val oldEmail: String? = null,
)

data class ChangeEmail(
    val newEmail: String,
    val oldEmail: String,
)

fun ChangeEmailPayload.toDomainModel() = ChangeEmail(
    newEmail = newEmail.orEmpty(),
    oldEmail = oldEmail.orEmpty(),
)