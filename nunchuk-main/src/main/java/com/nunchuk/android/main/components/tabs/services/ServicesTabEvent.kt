package com.nunchuk.android.main.components.tabs.services

import com.nunchuk.android.main.R
import com.nunchuk.android.model.*

sealed class ServicesTabEvent {
    data class ProcessFailure(val message: String) : ServicesTabEvent()
    data class GetServerKeySuccess(
        val signer: SingleSigner,
        val walletId: String,
        val token: String
    ) : ServicesTabEvent()

    data class Loading(val loading: Boolean) : ServicesTabEvent()
    data class CheckPasswordSuccess(val token: String, val item: ServiceTabRowItem) :
        ServicesTabEvent()

    data class LoadingEvent(val isLoading: Boolean) : ServicesTabEvent()
    data class CreateSupportRoomSuccess(val roomId: String) : ServicesTabEvent()
    data class CheckInheritance(val inheritanceCheck: InheritanceCheck): ServicesTabEvent()
}

data class ServicesTabState(
    val isPremiumUser: Boolean? = null,
    val isCreatedAssistedWallet: Boolean = false,
    val plan: MembershipPlan = MembershipPlan.NONE,
    val rowItems: List<Any> = emptyList(),
    val inheritance: Inheritance? = null
) {
    fun initRowItems(plan: MembershipPlan, inheritance: Inheritance? = null): List<Any> {
        val items = mutableListOf<Any>()
        when (plan) {
            MembershipPlan.NONE -> {
                items.add(NonSubHeader)
                items.add(
                    NonSubRow(
                        drawableId = R.drawable.ic_mulitsig_dark,
                        title = R.string.nc_no_single_point_failure,
                        desc = R.string.nc_no_single_point_failure_desc
                    )
                )
                items.add(
                    NonSubRow(
                        drawableId = R.drawable.ic_inheritance_planning,
                        title = R.string.nc_inheritance_planning,
                        desc = R.string.nc_inheritance_planning_desc
                    )
                )
                items.add(
                    NonSubRow(
                        drawableId = R.drawable.ic_emergency_lockdown_dark,
                        title = R.string.nc_emergency_lockdown,
                        desc = R.string.nc_emergency_lockdown_desc
                    )
                )
                items.add(
                    NonSubRow(
                        drawableId = R.drawable.ic_signing_policy,
                        title = R.string.nc_flexible_spending_policies,
                        desc = R.string.nc_flexible_spending_policies_desc
                    )
                )
                items.add(
                    NonSubRow(
                        drawableId = R.drawable.ic_key_recovery,
                        title = R.string.nc_cloud_backups_assisted_recovery,
                        desc = R.string.nc_cloud_backups_assisted_recovery_desc
                    )
                )
                items.add(
                    NonSubRow(
                        drawableId = R.drawable.ic_contact_support_dark,
                        title = R.string.nc_in_app_chat_support,
                        desc = R.string.nc_in_app_chat_support_desc
                    )
                )
                items.add(
                    NonSubRow(
                        drawableId = R.drawable.ic_member_discount,
                        title = R.string.nc_hardware_discounts,
                        desc = R.string.nc_hardware_discounts_desc
                    )
                )
            }
            MembershipPlan.IRON_HAND -> {
                items.add(ServiceTabRowCategory.Emergency)
                items.add(ServiceTabRowItem.KeyRecovery)
                items.add(ServiceTabRowCategory.Subscription)
                items.addAll(ServiceTabRowCategory.Subscription.items)
            }
            MembershipPlan.HONEY_BADGER -> {
                items.add(ServiceTabRowCategory.Emergency)
                items.addAll(ServiceTabRowCategory.Emergency.items)
                items.add(ServiceTabRowCategory.Inheritance)
                if (inheritance?.status == InheritanceStatus.PENDING_CREATION) {
                    items.add(ServiceTabRowItem.SetUpInheritancePlan)
                } else {
                    items.add(ServiceTabRowItem.ViewInheritancePlan)
                }
                items.add(ServiceTabRowItem.ClaimInheritance)
                items.add(ServiceTabRowCategory.Subscription)
                items.addAll(ServiceTabRowCategory.Subscription.items)
            }
        }
        return items
    }
}

data class NonSubRow(val drawableId: Int, val title: Int, val desc: Int)

object NonSubHeader

sealed class ServiceTabRowCategory(
    val title: Int,
    val drawableId: Int,
    val items: List<ServiceTabRowItem>
) {
    object Emergency :
        ServiceTabRowCategory(
            R.string.nc_emergency,
            R.drawable.ic_emergency,
            mutableListOf<ServiceTabRowItem>().apply {
                add(ServiceTabRowItem.EmergencyLockdown)
                add(ServiceTabRowItem.KeyRecovery)
            })

    object Inheritance :
        ServiceTabRowCategory(
            R.string.nc_inheritance_planning,
            R.drawable.ic_inheritance_planning,
            mutableListOf<ServiceTabRowItem>().apply {
                add(ServiceTabRowItem.SetUpInheritancePlan)
                add(ServiceTabRowItem.ClaimInheritance)
            })

    object Subscription :
        ServiceTabRowCategory(
            R.string.nc_your_subscription,
            R.drawable.ic_subscription,
            mutableListOf<ServiceTabRowItem>().apply {
                add(ServiceTabRowItem.CoSigningPolicies)
                add(ServiceTabRowItem.OrderNewHardware)
                add(ServiceTabRowItem.RollOverAssistedWallet)
                add(ServiceTabRowItem.ManageSubscription)
            })
}

sealed class ServiceTabRowItem(val category: ServiceTabRowCategory, val title: Int) {
    object EmergencyLockdown :
        ServiceTabRowItem(ServiceTabRowCategory.Emergency, R.string.nc_emergency_lockdown)

    object KeyRecovery :
        ServiceTabRowItem(ServiceTabRowCategory.Emergency, R.string.nc_key_recovery)

    object SetUpInheritancePlan :
        ServiceTabRowItem(ServiceTabRowCategory.Inheritance, R.string.nc_set_up_inheritance_plan)

    object ViewInheritancePlan :
        ServiceTabRowItem(ServiceTabRowCategory.Inheritance, R.string.nc_view_inheritance_plan)

    object ClaimInheritance :
        ServiceTabRowItem(ServiceTabRowCategory.Inheritance, R.string.nc_claim_an_inheritance)

    object CoSigningPolicies :
        ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_cosigning_policies)

    object OrderNewHardware :
        ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_order_new_hardware)

    object ManageSubscription :
        ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_manage_subscription)

    object RollOverAssistedWallet :
        ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_roll_over_assisted_wallet)
}