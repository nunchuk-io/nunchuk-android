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

package com.nunchuk.android.main.membership.authentication.dummytx

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgs
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationActivityArgs
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationEvent
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationViewModel
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.share.result.GlobalResultKey

class DummyTransactionIntroFragment : Fragment() {
    private val activityViewModel by activityViewModels<WalletAuthenticationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            val args: WalletAuthenticationActivityArgs by requireActivity().navArgs()
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val uiState by activityViewModel.state.collectAsStateWithLifecycle()
                val isGroup = !args.groupId.isNullOrEmpty()
                DummyTransactionIntroContent(
                    isGroup = isGroup,
                    pendingSignature = uiState.pendingSignature,
                    dummyTransactionType = uiState.dummyTransactionType,
                    onContinueClicked = {
                        if (isGroup && !args.dummyTransactionId.isNullOrEmpty()) {
                            activityViewModel.finalizeDummyTransaction(false)
                        } else {
                            findNavController().navigate(
                                DummyTransactionIntroFragmentDirections.actionDummyTransactionIntroToDummyTransactionDetailsFragment()
                            )
                        }
                    },
                    onCancelClicked = {
                        activityViewModel.finalizeDummyTransaction(true)
                    },
                    onRemoveDummyTransaction = {
                        activityViewModel.deleteDummyTransaction()
                        requireActivity().finish()
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(activityViewModel.event) {
            if (it is WalletAuthenticationEvent.FinalizeDummyTxSuccess) {
                if (it.isGoBack) {
                    requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(GlobalResultKey.DUMMY_TX_INTRO_DO_LATER, true)
                    })
                    requireActivity().finish()
                } else {
                    findNavController().navigate(
                        DummyTransactionIntroFragmentDirections.actionDummyTransactionIntroToDummyTransactionDetailsFragment()
                    )
                }
            }
        }
    }
}

@Composable
fun DummyTransactionIntroContent(
    isGroup: Boolean = false,
    pendingSignature: Int = 0,
    dummyTransactionType: DummyTransactionType = DummyTransactionType.NONE,
    onContinueClicked: () -> Unit = {},
    onCancelClicked: () -> Unit = {},
    onRemoveDummyTransaction: () -> Unit = {},
) {
    val title = when (dummyTransactionType) {
        DummyTransactionType.HEALTH_CHECK_REQUEST,
        DummyTransactionType.HEALTH_CHECK_PENDING,
        -> stringResource(R.string.nc_health_check_procedure)

        else -> stringResource(R.string.nc_signatures_required)
    }
    val firstSentence = when (dummyTransactionType) {
        DummyTransactionType.HEALTH_CHECK_REQUEST,
        DummyTransactionType.HEALTH_CHECK_PENDING,
        -> stringResource(R.string.nc_complete_a_health_check)
        DummyTransactionType.REQUEST_INHERITANCE_PLANNING -> stringResource(R.string.nc_authorize_inheritance_planning_request)

        else -> stringResource(R.string.nc_authorize_these_change)
    }
    val lastSentences = when {
        dummyTransactionType == DummyTransactionType.HEALTH_CHECK_REQUEST -> stringResource(R.string.nc_use_health_check_key_to_sign)
        dummyTransactionType == DummyTransactionType.HEALTH_CHECK_PENDING -> stringResource(R.string.nc_use_health_check_key_to_sign)
        isGroup && pendingSignature > 1 -> stringResource(id = R.string.nc_dummy_transaction_key_holder_desc)
        else -> ""
    }
    BackHandler {
        onRemoveDummyTransaction()
    }
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                NcTopAppBar(title = "", elevation = 0.dp)
                Text(
                    modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp),
                    text = title,
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(
                        R.string.nc_dummy_transaction_desc,
                        firstSentence,
                        Amount(value = 10000).getCurrencyAmount(),
                        lastSentences
                    )
                )
                Spacer(modifier = Modifier.weight(1.0f))
                Row(
                    modifier = Modifier
                        .align(CenterHorizontally)
                        .padding(bottom = 16.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pending_signatures),
                        contentDescription = "Icon pending signatures"
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = pluralStringResource(
                            R.plurals.nc_transaction_pending_signature,
                            pendingSignature,
                            pendingSignature
                        ), style = NunchukTheme.typography.bodySmall
                    )
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(R.string.nc_sign_dummy_transaction))
                }
                TextButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = onCancelClicked
                ) {
                    Text(
                        text = stringResource(R.string.nc_text_do_this_later),
                        style = NunchukTheme.typography.title
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DummyTransactionIntroContentPreview() {
    DummyTransactionIntroContent()
}