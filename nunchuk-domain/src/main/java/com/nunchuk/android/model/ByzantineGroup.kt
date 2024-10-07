package com.nunchuk.android.model

import android.os.Parcelable
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.ByzantinePreferenceSetup
import kotlinx.parcelize.Parcelize

@Parcelize
data class ByzantineGroup(
    val createdTimeMillis: Long,
    val id: String,
    val members: List<ByzantineMember>,
    val setupPreference: String,
    val status: String,
    val walletConfig: WalletConfig,
    val isViewPendingWallet: Boolean,
    val isLocked: Boolean,
    val slug: String,
) : Parcelable {
    fun isPendingWallet() = status == GroupStatus.PENDING_WALLET.name

    fun isSinglePersonSetup() = setupPreference == ByzantinePreferenceSetup.SINGLE_PERSON.name

    fun getMasterName() : String = members.find { it.role == AssistedWalletRole.MASTER.name }?.user?.name.orEmpty()

    fun isPremier() = slug.toMembershipPlan() == MembershipPlan.BYZANTINE_PREMIER
}