package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

/**
 * Body of `PUT .../{xfp}/claim-options`.
 *
 * The server has no `BOTH` value: "do both" is both entries. Empty lists, duplicates and
 * unsupported options are rejected, so the caller must send a validated, non-empty list.
 */
data class ClaimOptionsRequest(
    @SerializedName("claim_options")
    val claimOptions: List<String>,
)
