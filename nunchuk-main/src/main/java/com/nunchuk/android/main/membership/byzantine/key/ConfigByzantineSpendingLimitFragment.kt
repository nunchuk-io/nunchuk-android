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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NumberCommaTransformation
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.CurrencyFormatter
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.model.byzantine.InputSpendingPolicy
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.wallet.components.cosigning.CosigningGroupPolicyFragmentArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfigByzantineSpendingLimitFragment : MembershipFragment(), BottomSheetOptionListener {
    private val viewModel: ConfigByzantineSpendingLimitViewModel by viewModels()
    private val args: ConfigByzantineSpendingLimitFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ConfigSpendingLimitScreen(viewModel = viewModel,
                    args = args,
                    onMoreClicked = ::handleShowMore,
                    onShowCurrencyUnitOption = {
                        showCurrencyUnitOption(it)
                    },
                    onShowTimeUnitOption = {
                        showTimeUnitOption(it)
                    })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is ConfigByzantineSpendingLimitEvent.ContinueClicked -> handleContinueClicked(
                            event
                        )

                        is ConfigByzantineSpendingLimitEvent.Error -> showError(event.message)
                        is ConfigByzantineSpendingLimitEvent.Loading -> showOrHideLoading(event.isLoading)
                    }
                }
        }
    }

    private fun handleContinueClicked(event: ConfigByzantineSpendingLimitEvent.ContinueClicked) {
        if (!args.xfp.isNullOrEmpty()) {
            // edit mode
            val newArgs = CosigningGroupPolicyFragmentArgs(
                keyPolicy = event.keyPolicy,
            )
            requireActivity().apply {
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtras(newArgs.toBundle())
                })
                finish()
            }
        } else {
            // create mode
            findNavController().navigate(
                ConfigByzantineSpendingLimitFragmentDirections.actionConfigByzantineSpendingLimitFragmentToConfigureByzantineServerKeySettingFragment(
                    keyPolicy = event.keyPolicy,
                    groupId = args.groupId,
                    preferenceSetup = viewModel.getPreferenceSetup()
                )
            )
        }
    }

    private fun showCurrencyUnitOption(email: String?) {
        val policy = viewModel.getPolicyByEmail(email) ?: return
        viewModel.setCurrentEmailInteract(email)
        BottomSheetOption.newInstance(
            SpendingCurrencyUnit.values().map {
                SheetOption(
                    type = OFFSET + it.ordinal,
                    label = it.toLabel(requireContext()),
                    isSelected = it.toLabel(requireContext()) == policy.currencyUnit
                )
            }
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showTimeUnitOption(email: String?) {
        val policy = viewModel.getPolicyByEmail(email) ?: return
        viewModel.setCurrentEmailInteract(email)
        BottomSheetOption.newInstance(
            SpendingTimeUnit.values().map {
                SheetOption(
                    type = it.ordinal,
                    label = it.toLabel(requireContext()),
                    isSelected = it == policy.timeUnit
                )
            }
        ).show(childFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        if (option.type >= OFFSET) {
            val type = SpendingCurrencyUnit.values()[option.type - OFFSET]
            viewModel.setCurrencyUnit(type.toLabel(requireContext()))
        } else {
            val type = SpendingTimeUnit.values()[option.type]
            viewModel.setTimeUnit(type)
        }
    }

    companion object {
        private const val OFFSET = 10000
    }
}

@Composable
private fun ConfigSpendingLimitScreen(
    viewModel: ConfigByzantineSpendingLimitViewModel,
    args: ConfigByzantineSpendingLimitFragmentArgs,
    onShowTimeUnitOption: (email: String?) -> Unit = {},
    onShowCurrencyUnitOption: (email: String?) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    ConfigSpendingLimitContent(
        remainTime = remainTime,
        state = state,
        onShowCurrencyUnitOption = onShowCurrencyUnitOption,
        onShowTimeUnitOption = onShowTimeUnitOption,
        onContinueClicked = viewModel::onContinueClicked,
        onMoreClicked = onMoreClicked,
        onLimitChange = viewModel::setSpendingLimit,
        isEditMode = !args.xfp.isNullOrEmpty()
    )
}

@Composable
private fun ConfigSpendingLimitContent(
    remainTime: Int = 0,
    state: ConfigMemberSpendingLimitState = ConfigMemberSpendingLimitState(),
    onContinueClicked: (isApplyToAllMember: Boolean) -> Unit = {},
    onShowTimeUnitOption: (email: String?) -> Unit = {},
    onShowCurrencyUnitOption: (email: String?) -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onLimitChange: (email: String?, limit: String) -> Unit = { _, _ -> },
    isEditMode: Boolean = false,
) {
    var isApplyToAllMember by rememberSaveable(state.isApplyToAllMember) {
        mutableStateOf(state.isApplyToAllMember)
    }
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(
                    title = if (isEditMode) "" else stringResource(
                        R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                    actions = {
                        if (!isEditMode) {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = com.nunchuk.android.signer.R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        }
                    }
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.nc_config_cosiging_spending_limit),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    text = stringResource(R.string.nc_cosigning_pending_limit_desc),
                    style = NunchukTheme.typography.body
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.nc_apply_the_same_limit),
                        style = NunchukTheme.typography.body
                    )
                    Switch(checked = isApplyToAllMember, onCheckedChange = {
                        isApplyToAllMember = isApplyToAllMember.not()
                    })
                }
                val policy = state.policies[null]?.spendingPolicy
                if (isApplyToAllMember && policy != null) {
                    ConfigPolicyForAllMemberView(
                        policy,
                        onLimitChange,
                        onShowCurrencyUnitOption,
                        onShowTimeUnitOption
                    )
                    Spacer(modifier = Modifier.Companion.weight(1.0f))
                }
                val memberPolicies = state.policies.filter { it.key != null }.values.toList()
                if (isApplyToAllMember.not() && memberPolicies.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(memberPolicies) { index, policy ->
                            SpendingLimitAccountView(
                                member = policy,
                                onShowTimeUnitOption = onShowTimeUnitOption,
                                onShowCurrencyUnitOption = onShowCurrencyUnitOption,
                                index = index,
                                onLimitChange = onLimitChange
                            )
                        }
                    }
                }
                NcHintMessage(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_hint_config_spending_limit)))
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        onContinueClicked(isApplyToAllMember)
                    },
                ) {
                    Text(
                        text = if (isEditMode) stringResource(R.string.nc_update_spending_limit) else stringResource(
                            id = R.string.nc_text_continue
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigPolicyForAllMemberView(
    policy: InputSpendingPolicy,
    onLimitChange: (email: String?, limit: String) -> Unit,
    onShowCurrencyUnitOption: (email: String?) -> Unit,
    onShowTimeUnitOption: (email: String?) -> Unit
) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = stringResource(R.string.nc_cosigning_spending_limit),
        style = NunchukTheme.typography.titleSmall
    )
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Max)
    ) {
        NcTextField(
            modifier = Modifier
                .weight(1.0f)
                .padding(end = 16.dp),
            title = "",
            value = policy.limit,
            visualTransformation = NumberCommaTransformation(),
            onValueChange = {
                onLimitChange(null, CurrencyFormatter.format(it, 2).take(15))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .border(
                    width = 1.dp,
                    color = Color(0xFFDEDEDE),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable { onShowCurrencyUnitOption(null) }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(end = 16.dp),
                text = policy.currencyUnit,
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow),
                contentDescription = ""
            )
        }
    }
    Row(
        modifier = Modifier
            .padding(16.dp)
            .height(52.dp)
            .clickable { onShowTimeUnitOption(null) }
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFDEDEDE),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(end = 16.dp),
            text = policy.timeUnit.toLabel(LocalContext.current),
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow),
            contentDescription = ""
        )
    }
}

@Preview
@Composable
private fun ConfigSpendingLimitScreenPreview() {
    ConfigSpendingLimitContent()
}