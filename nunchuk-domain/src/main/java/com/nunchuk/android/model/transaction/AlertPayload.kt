package com.nunchuk.android.model.transaction

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlertPayload(
    @SerializedName("master_name")
    val masterName: String,
    @SerializedName("pending_keys_count")
    val pendingKeysCount: Int,
    @SerializedName("dummy_transaction_id")
    val dummyTransactionId: String = "",
    @SerializedName("xfps")
    val xfps: List<String> = emptyList(),
    @SerializedName("claim_key")
    val claimKey: Boolean = false,
    @SerializedName("key_xfp")
    val keyXfp: String = "",
    @SerializedName("payment_name")
    val paymentName: String? = null,
    @SerializedName("request_id")
    val requestId: String = "",
    @SerializedName("membership_id")
    val membershipId: String = "",
    @SerializedName("transaction_id")
    val transactionId: String = "",
    @SerializedName("can_cancel")
    val canCancel: Boolean = false,
    @SerializedName("can_replace")
    val canReplace: Boolean = false,
    @SerializedName("xfp")
    val xfp: String = "",
    @SerializedName("new_wallet_local_id")
    val newWalletId: String = "",
) : Parcelable