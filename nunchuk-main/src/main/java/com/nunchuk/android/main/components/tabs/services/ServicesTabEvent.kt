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

package com.nunchuk.android.main.components.tabs.services

import android.os.Parcelable
import com.nunchuk.android.main.R
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritanceCheck
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.banner.BannerPage
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.membership.AssistedWalletBrief
import kotlinx.parcelize.Parcelize

sealed class ServicesTabEvent {
    data class ProcessFailure(val message: String) : ServicesTabEvent()
    data class GetServerKeySuccess(
        val signer: SingleSigner,
        val walletId: String,
        val token: String
    ) : ServicesTabEvent()

    data class Loading(val loading: Boolean) : ServicesTabEvent()
    data class CheckPasswordSuccess(
        val token: String,
        val walletId: String,
        val item: ServiceTabRowItem,
        val groupId: String?
    ) : ServicesTabEvent()

    data class CreateSupportRoomSuccess(val roomId: String) : ServicesTabEvent()
    data class CheckInheritance(val inheritanceCheck: InheritanceCheck) : ServicesTabEvent()
    data object EmailInvalid : ServicesTabEvent()
    data class OnSubmitEmailSuccess(val email: String) : ServicesTabEvent()
    data class GetInheritanceSuccess(
        val walletId: String,
        val inheritance: Inheritance,
        val token: String,
        val groupId: String?
    ) : ServicesTabEvent()

    data class OpenSetupInheritancePlan(val walletId: String, val groupId: String?) :
        ServicesTabEvent()
    data class CalculateRequiredSignaturesSuccess(
        val type: String,
        val walletId: String,
        val groupId: String
    ) : ServicesTabEvent()
}

data class ServicesTabState(
    val isPremiumUser: Boolean? = null,
    val plan: MembershipPlan = MembershipPlan.NONE,
    val assistedWallets: List<AssistedWalletBrief> = emptyList(),
    val banner: Banner? = null,
    val bannerPage: BannerPage? = null,
    val allGroups : List<ByzantineGroup> = emptyList(),
    val joinedGroups: Map<String, ByzantineGroup> = mutableMapOf(),
    val groups2of4Multisig: List<ByzantineGroup> = emptyList(),
    val userRole: String = AssistedWalletRole.NONE.name,
    val isMasterHasNotCreatedWallet: Boolean = false,
) {
    fun initRowItems(): List<Any> {
        val items = mutableListOf<Any>()
        when (plan) {
            MembershipPlan.NONE -> {
                if (allGroups.isNotEmpty()) {
                    if (isShowEmptyState()) items.add(EmptyState)
                    else if (hasPremierGroupWallet()) return getItemsByzantinePremier()
                    else return getItemsByzantineAndPro()
                } else {
                    bannerPage?.let { bannerPage ->
                        items.add(NonSubHeader(title = bannerPage.title, desc = bannerPage.desc))
                        bannerPage.items.forEach {
                            items.add(NonSubRow(url = it.url, title = it.title, desc = it.desc))
                        }
                    }
                }
                return items
            }

            MembershipPlan.IRON_HAND -> {
                items.apply {
                    add(ServiceTabRowCategory.Emergency)
                    add(ServiceTabRowItem.EmergencyLockdown)
                    add(ServiceTabRowItem.KeyRecovery)
                    add(ServiceTabRowCategory.Inheritance)
                    add(ServiceTabRowItem.ClaimInheritance)
                    add(ServiceTabRowCategory.Subscription)
                    if (assistedWallets.isNotEmpty()) {
                        add(ServiceTabRowItem.CoSigningPolicies)
                    }
                    add(ServiceTabRowItem.OrderNewHardware)
                    add(ServiceTabRowItem.RollOverAssistedWallet)
                    add(ServiceTabRowItem.ManageSubscription)
                }
                if (banner != null) {
                    items.add(Banner(banner.id, banner.url, banner.title))
                }
                return items
            }

            MembershipPlan.HONEY_BADGER -> {
                items.apply {
                    add(ServiceTabRowCategory.Emergency)
                    add(ServiceTabRowItem.EmergencyLockdown)
                    add(ServiceTabRowItem.KeyRecovery)
                    add(ServiceTabRowCategory.Inheritance)
                    if (assistedWallets.isEmpty() || assistedWallets.all { it.isSetupInheritance.not() }) {
                        add(ServiceTabRowItem.SetUpInheritancePlan)
                    } else {
                        add(ServiceTabRowItem.ViewInheritancePlan)
                    }
                    add(ServiceTabRowItem.ClaimInheritance)
                    add(ServiceTabRowCategory.Subscription)
                    if (assistedWallets.isNotEmpty()) {
                        add(ServiceTabRowItem.CoSigningPolicies)
                    }
                    add(ServiceTabRowItem.OrderNewHardware)
                    add(ServiceTabRowItem.RollOverAssistedWallet)
                    add(ServiceTabRowItem.ManageSubscription)
                }
                return items
            }

            MembershipPlan.BYZANTINE_PREMIER -> return getItemsByzantinePremier()

            MembershipPlan.BYZANTINE, MembershipPlan.BYZANTINE_PRO -> {
                return if (hasPremierGroupWallet()) getItemsByzantinePremier() else getItemsByzantineAndPro()
            }

            else -> {}
        }
        return items
    }

    private fun getItemsByzantineAndPro(): List<Any> {
        val items = mutableListOf<Any>()
        if (isShowEmptyState()) items.add(EmptyState)
        else if (userRole == AssistedWalletRole.OBSERVER.name) {
            items.add(ObserverRole)
            return items
        } else if (userRole == AssistedWalletRole.KEYHOLDER_LIMITED.name ||
            userRole == AssistedWalletRole.KEYHOLDER.name && groups2of4Multisig.isEmpty()
        ) {
            items.apply {
                add(ServiceTabRowCategory.Emergency)
                add(ServiceTabRowItem.KeyRecovery)
                add(ServiceTabRowCategory.Inheritance)
                add(ServiceTabRowItem.ClaimInheritance)
            }
            return items
        } else if (groups2of4Multisig.isNotEmpty()) {
            if (userRole == AssistedWalletRole.KEYHOLDER.name) {
                items.apply {
                    add(ServiceTabRowCategory.Emergency)
                    add(ServiceTabRowItem.KeyRecovery)
                    add(ServiceTabRowCategory.Inheritance)
                    if (isHasSetupInheritanceWallet()) {
                        add(ServiceTabRowItem.ViewInheritancePlan)
                    }
                    add(ServiceTabRowItem.ClaimInheritance)
                    showOption2Of4Multisig {
                        add(ServiceTabRowCategory.Subscription)
                        add(ServiceTabRowItem.CoSigningPolicies)
                    }
                }
                return items
            } else if (userRole == AssistedWalletRole.ADMIN.name
                || userRole == AssistedWalletRole.MASTER.name
            ) {
                items.apply {
                    add(ServiceTabRowCategory.Emergency)
                    add(ServiceTabRowItem.EmergencyLockdown)
                    add(ServiceTabRowItem.KeyRecovery)
                    add(ServiceTabRowCategory.Inheritance)
                    if (assistedWallets.isEmpty() ||
                        assistedWallets.filter { wallet -> groups2of4Multisig.find { wallet.groupId == it.id } != null }
                            .all { wallet -> wallet.isSetupInheritance.not() }
                    ) {
                        add(ServiceTabRowItem.SetUpInheritancePlan)
                    } else if (isHasSetupInheritanceWallet()) {
                        add(ServiceTabRowItem.ViewInheritancePlan)
                    }
                    add(ServiceTabRowItem.ClaimInheritance)
                    if (userRole == AssistedWalletRole.MASTER.name) {
                        add(ServiceTabRowCategory.Subscription)
                        showOption2Of4Multisig {
                            add(ServiceTabRowItem.CoSigningPolicies)
                        }
                        add(ServiceTabRowItem.GetAdditionalWallets)
                        add(ServiceTabRowItem.RollOverAssistedWallet)
                        add(ServiceTabRowItem.ManageSubscription)
                    } else {
                        showOption2Of4Multisig {
                            add(ServiceTabRowCategory.Subscription)
                            add(ServiceTabRowItem.CoSigningPolicies)
                        }
                    }
                }
                return items
            }
        } else {
            if (userRole == AssistedWalletRole.ADMIN.name) {
                items.apply {
                    add(ServiceTabRowCategory.Emergency)
                    add(ServiceTabRowItem.EmergencyLockdown)
                    add(ServiceTabRowItem.KeyRecovery)
                    add(ServiceTabRowCategory.Inheritance)
                    add(ServiceTabRowItem.ClaimInheritance)
                }
                return items
            } else if (userRole == AssistedWalletRole.MASTER.name) {
                items.apply {
                    add(ServiceTabRowCategory.Emergency)
                    add(ServiceTabRowItem.EmergencyLockdown)
                    add(ServiceTabRowItem.KeyRecovery)
                    add(ServiceTabRowCategory.Inheritance)
                    add(ServiceTabRowItem.ClaimInheritance)
                    add(ServiceTabRowCategory.Subscription)
                    add(ServiceTabRowItem.GetAdditionalWallets)
                    add(ServiceTabRowItem.RollOverAssistedWallet)
                    add(ServiceTabRowItem.ManageSubscription)
                }
                return items
            }
        }
        return items
    }

    private fun getItemsByzantinePremier(): List<Any> {
        val items = mutableListOf<Any>()
        if (isShowEmptyState()) {
            items.add(EmptyState)
            return items
        } else if (userRole == AssistedWalletRole.OBSERVER.name) {
            items.add(ObserverRole)
            return items
        } else if (userRole == AssistedWalletRole.KEYHOLDER_LIMITED.name) {
            items.apply {
                add(ServiceTabRowCategory.Emergency)
                add(ServiceTabRowItem.KeyRecovery)
            }
            return items
        } else if (userRole == AssistedWalletRole.KEYHOLDER.name) {
            items.apply {
                add(ServiceTabRowCategory.Emergency)
                add(ServiceTabRowItem.KeyRecovery)
                showOption2Of4Multisig {
                    add(ServiceTabRowCategory.Subscription)
                    add(ServiceTabRowItem.CoSigningPolicies)
                }
            }
            return items
        } else if (userRole == AssistedWalletRole.MASTER.name) {
            items.apply {
                add(ServiceTabRowCategory.Emergency)
                add(ServiceTabRowItem.EmergencyLockdown)
                add(ServiceTabRowItem.KeyRecovery)
                add(ServiceTabRowCategory.Subscription)
                showOption2Of4Multisig {
                    add(ServiceTabRowItem.CoSigningPolicies)
                }
                add(ServiceTabRowItem.RollOverAssistedWallet)
                add(ServiceTabRowItem.ManageSubscription)
            }
            return items
        } else if (userRole == AssistedWalletRole.ADMIN.name) {
            items.apply {
                add(ServiceTabRowCategory.Emergency)
                add(ServiceTabRowItem.EmergencyLockdown)
                add(ServiceTabRowItem.KeyRecovery)
                showOption2Of4Multisig {
                    add(ServiceTabRowCategory.Subscription)
                    add(ServiceTabRowItem.CoSigningPolicies)
                }
            }
            return items
        }
        return items
    }

    private fun hasPremierGroupWallet(): Boolean {
        return allGroups.any { group -> group.isPremier() }
    }

    private fun isShowEmptyState(): Boolean {
       if (plan == MembershipPlan.NONE && allGroups.isNotEmpty() && joinedGroups.isEmpty()) return true
        if (allGroups.isEmpty()) return true
        if (isMasterHasNotCreatedWallet) return true
        return false
    }

    private fun showOption2Of4Multisig(block: () -> Unit) {
        if (groups2of4Multisig.isNotEmpty() && assistedWallets.any { wallet -> groups2of4Multisig.find { wallet.groupId == it.id } != null }) {
            block.invoke()
        }
    }

    private fun isHasSetupInheritanceWallet(): Boolean {
        return assistedWallets.any { it.isSetupInheritance }
    }
}

internal data class Banner(val id: String, val url: String, val title: String)

internal data class NonSubRow(val url: String, val title: String, val desc: String)

internal data class NonSubHeader(val title: String, val desc: String)

internal data object EmptyState

internal data object ObserverRole

sealed class ServiceTabRowCategory(val title: Int, val drawableId: Int) {
    data object Emergency : ServiceTabRowCategory(R.string.nc_emergency, R.drawable.ic_emergency)
    data object Inheritance :
        ServiceTabRowCategory(R.string.nc_inheritance_planning, R.drawable.ic_inheritance_planning)

    data object Subscription :
        ServiceTabRowCategory(R.string.nc_your_subscription, R.drawable.ic_subscription)
}

sealed class ServiceTabRowItem(val title: Int) : Parcelable {
    @Parcelize
    data object EmergencyLockdown : ServiceTabRowItem(R.string.nc_emergency_lockdown)

    @Parcelize
    data object KeyRecovery : ServiceTabRowItem(R.string.nc_key_recovery)

    @Parcelize
    data object SetUpInheritancePlan : ServiceTabRowItem(R.string.nc_set_up_inheritance_plan)

    @Parcelize
    data object ViewInheritancePlan : ServiceTabRowItem(R.string.nc_view_inheritance_plan)

    @Parcelize
    data object ClaimInheritance : ServiceTabRowItem(R.string.nc_claim_an_inheritance)

    @Parcelize
    data object CoSigningPolicies : ServiceTabRowItem(R.string.nc_cosigning_policies)

    @Parcelize
    data object OrderNewHardware : ServiceTabRowItem(R.string.nc_order_new_hardware)

    @Parcelize
    data object ManageSubscription : ServiceTabRowItem(R.string.nc_manage_subscription)

    @Parcelize
    data object RollOverAssistedWallet : ServiceTabRowItem(R.string.nc_roll_over_assisted_wallet)

    @Parcelize
    data object GetAdditionalWallets : ServiceTabRowItem(R.string.nc_get_additional_wallet)
}