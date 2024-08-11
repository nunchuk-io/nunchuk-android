package com.nunchuk.android.core.referral

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Keep
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.ReferrerCode
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import kotlinx.parcelize.Parcelize

@Keep
enum class ReferralFlow {
    SETUP, EDIT
}

@Parcelize
data class ReferralArgs(
    val flow: ReferralFlow = ReferralFlow.SETUP,
    val campaign: Campaign,
    val localReferrerCode: ReferrerCode? = null
) : Parcelable {
    companion object {
        private const val EXTRA_FLOW = "EXTRA_FLOW"
        private const val EXTRA_CAMPAIGN = "EXTRA_CAMPAIGN"
        private const val EXTRA_LOCAL_REFERRER_CODE = "EXTRA_LOCAL_REFERRER_CODE"

        fun fromBundle(bundle: Bundle): ReferralArgs {
            return ReferralArgs(
                flow = bundle.serializable<ReferralFlow>(EXTRA_FLOW)!!,
                campaign = bundle.parcelable(EXTRA_CAMPAIGN) ?: error("Campaign is required"),
                localReferrerCode = bundle.parcelable(EXTRA_LOCAL_REFERRER_CODE)
            )
        }

        val default: ReferralArgs
            get() = ReferralArgs(
                flow = ReferralFlow.SETUP,
                campaign = Campaign.empty(),
                localReferrerCode = null
            )
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putSerializable(EXTRA_FLOW, flow)
            putParcelable(EXTRA_CAMPAIGN, campaign)
            putParcelable(EXTRA_LOCAL_REFERRER_CODE, localReferrerCode)
        }
    }
}