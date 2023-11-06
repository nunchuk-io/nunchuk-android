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

package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownperiod

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.components.tabs.services.emergencylockdown.EmergencyLockdownActivity
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.R
import com.nunchuk.android.utils.serializable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EmergencyLockdownPeriodFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: EmergencyLockdownPeriodViewModel by viewModels()
    private val groupId by lazy { (requireActivity() as? EmergencyLockdownActivity)?.groupId.orEmpty() }
    private val verifyToken by lazy { (requireActivity() as? EmergencyLockdownActivity)?.verifyToken.orEmpty() }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val signatureMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.SIGNATURE_EXTRA)
                        ?: return@registerForActivityResult
                val securityQuestionToken =
                    data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                val confirmCodeMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.CONFIRM_CODE)
                        .orEmpty()
                viewModel.lockdownUpdate(
                    signatureMap,
                    securityQuestionToken,
                    confirmCodeMap[GlobalResultKey.CONFIRM_CODE_TOKEN].orEmpty(),
                    confirmCodeMap[GlobalResultKey.CONFIRM_CODE_NONCE].orEmpty()
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                LockdownPeriodScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(verifyToken, groupId)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is LockdownPeriodEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is LockdownPeriodEvent.ProcessFailure -> showError(message = event.message)
                is LockdownPeriodEvent.CalculateRequiredSignaturesSuccess -> navigator.openWalletAuthentication(
                    walletId = event.walletId,
                    userData = event.userData,
                    requiredSignatures = event.requiredSignatures,
                    type = event.type,
                    launcher = launcher,
                    action = TargetAction.EMERGENCY_LOCKDOWN.name,
                    activityContext = requireActivity()
                )

                is LockdownPeriodEvent.LockdownUpdateSuccess -> {
                    findNavController().navigate(
                        EmergencyLockdownPeriodFragmentDirections.actionLockdownPeriodFragmentToLockdownSuccessFragment(
                            period = event.period
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LockdownPeriodScreen(
    viewModel: EmergencyLockdownPeriodViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LockdownPeriodContent(
        state.options,
        onOptionClick = viewModel::onOptionClick,
        onContinueClicked = viewModel::calculateRequiredSignatures
    )
}

@Composable
private fun LockdownPeriodContent(
    options: List<PeriodOption> = emptyList(),
    onOptionClick: (String) -> Unit = {},
    onContinueClicked: () -> Unit = {}
) = NunchukTheme {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            NcTopAppBar(title = "")
            LazyColumn(
                modifier = Modifier.weight(1F),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.nc_emergency_lockdown_period_title),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(R.string.nc_emergency_lockdown_period_desc),
                        style = NunchukTheme.typography.body,
                    )
                }

                items(options) { item ->
                    OptionItem(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = item.isSelected,
                        label = item.period.displayName
                    ) {
                        onOptionClick(item.period.id)
                    }
                }
            }

            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = options.any { it.isSelected },
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier, onClick = onClick,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) colorResource(id = R.color.nc_primary_color) else Color(
                0xFFDEDEDE
            )
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick)
            Text(text = label, style = NunchukTheme.typography.title)
        }
    }
}

@Preview
@Composable
private fun EmergencyLockdownPeriodScreenPreview() {
    LockdownPeriodContent()
}