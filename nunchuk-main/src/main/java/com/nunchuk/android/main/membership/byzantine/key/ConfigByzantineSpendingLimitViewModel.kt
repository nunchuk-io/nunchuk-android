/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.main.membership.byzantine.key

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.main.R
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedMemberSpendingPolicy
import com.nunchuk.android.model.byzantine.ByzantinePreferenceSetup
import com.nunchuk.android.model.byzantine.InputSpendingPolicy
import com.nunchuk.android.model.byzantine.isKeyHolder
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.toByzantinePreferenceSetup
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.util.LoadingOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols
import javax.inject.Inject

@HiltViewModel
class ConfigByzantineSpendingLimitViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    private val savedStateHandle: SavedStateHandle,
    private val getGroupUseCase: GetGroupUseCase,
) : ViewModel() {
    private val args =
        ConfigByzantineSpendingLimitFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<ConfigByzantineSpendingLimitEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ConfigMemberSpendingLimitState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    init {
        val keyPolicy = args.keyPolicy
        _state.update { it.copy(isApplyToAllMember = keyPolicy?.isApplyAll ?: false) }
        if (keyPolicy != null && keyPolicy.isApplyAll && keyPolicy.spendingPolicies.isNotEmpty()) {
            val spendingLimit = keyPolicy.spendingPolicies.values.first()
            _state.update {
                it.copy(
                    policies = mapOf(
                        null to AssistedMemberSpendingPolicy(
                            spendingPolicy = map(spendingLimit)
                        )
                    ),
                )
            }
        }
        viewModelScope.launch {
            _event.emit(ConfigByzantineSpendingLimitEvent.Loading(true))
            getGroupUseCase(GetGroupUseCase.Params(args.groupId, loadingOptions = LoadingOptions.REMOTE)).collect {
                _event.emit(ConfigByzantineSpendingLimitEvent.Loading(false))
                if (it.isFailure) {
                    _event.emit(ConfigByzantineSpendingLimitEvent.Error(it.exceptionOrNull()?.message.orEmpty()))
                    return@collect
                }
                val group = it.getOrThrow()
                val spendingLimits = keyPolicy?.spendingPolicies.orEmpty()
                val newPolicies = group.members.mapNotNull { member ->
                    val role = member.role.toRole
                    val policy = spendingLimits[member.membershipId]?.let { spendingLimit ->
                        map(spendingLimit)
                    } ?: InputSpendingPolicy(
                        limit = if (role.isKeyHolderLimited) "0" else "5000", SpendingTimeUnit.DAILY, LOCAL_CURRENCY
                    )
                    if (role.isKeyHolder) {
                        AssistedMemberSpendingPolicy(
                            member = AssistedMember(
                                role = member.role,
                                email = member.emailOrUsername,
                                name = member.user?.name,
                                membershipId = member.membershipId
                            ),
                            spendingPolicy = policy,
                            isJoinGroup = member.isContact(),
                        )
                    } else null
                }.associateBy { policy -> policy.member?.email }
                val combinePolicies = _state.value.policies.toMutableMap().apply {
                    putAll(newPolicies)
                }
                _state.update { state ->
                    state.copy(
                        policies = combinePolicies,
                        preferenceSetup = group.setupPreference.toByzantinePreferenceSetup()
                    )
                }
            }
        }
    }

    fun onContinueClicked(isApplyToAllMember: Boolean) {
        val groupKeyPolicy = args.keyPolicy ?: GroupKeyPolicy()
        viewModelScope.launch {
            val state = state.value
            if (isApplyToAllMember) {
                state.policies[null]?.spendingPolicy?.let { policy ->
                    _event.emit(
                        ConfigByzantineSpendingLimitEvent.ContinueClicked(
                            groupKeyPolicy.copy(
                                isApplyAll = true,
                                spendingPolicies = mapOf(
                                    "" to SpendingPolicy(
                                        limit = policy.limit.toDoubleOrNull() ?: 0.0,
                                        timeUnit = policy.timeUnit,
                                        currencyUnit = policy.currencyUnit,
                                    ),
                                )
                            )
                        )
                    )
                }
            } else {
                val memberPolicies = state.policies.filter { it.key != null }
                _event.emit(
                    ConfigByzantineSpendingLimitEvent.ContinueClicked(
                        groupKeyPolicy.copy(
                            isApplyAll = false,
                            spendingPolicies = memberPolicies.values.associate { memberPolicy ->
                                memberPolicy.member?.membershipId.orEmpty() to SpendingPolicy(
                                    limit = memberPolicy.spendingPolicy.limit.toDoubleOrNull()
                                        ?: 0.0,
                                    timeUnit = memberPolicy.spendingPolicy.timeUnit,
                                    currencyUnit = memberPolicy.spendingPolicy.currencyUnit,
                                )
                            }
                        )
                    )
                )
            }
        }
    }

    private fun map(spendingLimit: SpendingPolicy) = InputSpendingPolicy(
        limit = spendingLimit.limit.formatDecimalWithoutZero(
            USD_FRACTION_DIGITS
        ).replace(DecimalFormatSymbols.getInstance().groupingSeparator.toString(), ""),
        spendingLimit.timeUnit,
        spendingLimit.currencyUnit
    )

    fun setCurrentEmailInteract(email: String?) {
        savedStateHandle[KEY_EMAIL] = email
    }

    fun getPolicyByEmail(email: String?) = _state.value.policies[email]?.spendingPolicy

    fun setSpendingLimit(email: String?, limit: String) {
        val newPolicies = _state.value.policies.toMutableMap()
        newPolicies[email]?.let { policy ->
            val newPolicy = policy.copy(spendingPolicy = policy.spendingPolicy.copy(limit = limit))
            newPolicies[email] = newPolicy
            _state.update { it.copy(policies = newPolicies) }
        }
    }

    fun setTimeUnit(unit: SpendingTimeUnit) {
        val newPolicies = _state.value.policies.toMutableMap()
        val email = savedStateHandle.get<String>(KEY_EMAIL)
        newPolicies[email]?.let { policy ->
            val newPolicy =
                policy.copy(spendingPolicy = policy.spendingPolicy.copy(timeUnit = unit))
            newPolicies[email] = newPolicy
            _state.update { it.copy(policies = newPolicies) }
        }
    }

    fun setCurrencyUnit(unit: String) {
        val newPolicies = _state.value.policies.toMutableMap()
        val email = savedStateHandle.get<String>(KEY_EMAIL)
        newPolicies[email]?.let { policy ->
            val newPolicy =
                policy.copy(spendingPolicy = policy.spendingPolicy.copy(currencyUnit = unit))
            newPolicies[email] = newPolicy
            _state.update { it.copy(policies = newPolicies) }
        }
    }

    fun getPreferenceSetup() = _state.value.preferenceSetup

    companion object {
        const val KEY_EMAIL = "email"
    }
}

data class ConfigMemberSpendingLimitState(
    val policies: Map<String?, AssistedMemberSpendingPolicy> = mapOf(
        null to AssistedMemberSpendingPolicy(
            spendingPolicy = InputSpendingPolicy(
                limit = "5000", SpendingTimeUnit.DAILY, LOCAL_CURRENCY
            )
        )
    ),
    val preferenceSetup: ByzantinePreferenceSetup = ByzantinePreferenceSetup.SINGLE_PERSON,
    val isApplyToAllMember: Boolean = false,
)

sealed class ConfigByzantineSpendingLimitEvent {
    data class ContinueClicked(
        val keyPolicy: GroupKeyPolicy
    ) : ConfigByzantineSpendingLimitEvent()

    data class Loading(val isLoading: Boolean = false) : ConfigByzantineSpendingLimitEvent()
    data class Error(val message: String) : ConfigByzantineSpendingLimitEvent()
}

fun SpendingCurrencyUnit.toLabel(context: Context) = when (this) {
    SpendingCurrencyUnit.CURRENCY_UNIT -> LOCAL_CURRENCY
    SpendingCurrencyUnit.BTC -> context.getString(R.string.nc_currency_btc)
    SpendingCurrencyUnit.sat -> context.getString(R.string.nc_currency_sat)
}

fun SpendingTimeUnit.toLabel(context: Context) = when (this) {
    SpendingTimeUnit.DAILY -> context.getString(R.string.nc_daily)
    SpendingTimeUnit.MONTHLY -> context.getString(R.string.nc_monthly)
    SpendingTimeUnit.WEEKLY -> context.getString(R.string.nc_weekly)
    SpendingTimeUnit.YEARLY -> context.getString(R.string.nc_yearly)
}