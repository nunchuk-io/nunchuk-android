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

package com.nunchuk.android.wallet.components.review

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.CreateWalletErrorEvent
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.CreateWalletSuccessEvent
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.SetLoadingEvent
import com.nunchuk.android.wallet.util.isWalletExisted
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReviewWalletActivity : BaseComposeActivity() {

    @Inject
    internal lateinit var vmFactory: ReviewWalletViewModel.Factory

    private val viewModel: ReviewWalletViewModel by viewModels {
        viewModelProviderFactoryOf {
            vmFactory.create(args)
        }
    }

    private val args: ReviewWalletArgs by lazy { ReviewWalletArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ReviewWalletContent(
                args = args,
                signers = uiState.signers,
                uiState = uiState,
                onContinue = {
                    viewModel.handleContinueEvent()
                },
                openMainScreen = {
                    navigator.openMainScreen(this)
                }
            )
        }

        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: ReviewWalletEvent) {
        when (event) {
            is SetLoadingEvent -> showOrHideLoading(event.showLoading)
            is CreateWalletSuccessEvent -> onCreateWalletSuccess(event)
            is CreateWalletErrorEvent -> onCreateWalletError(event)
            is ReviewWalletEvent.CreateFreeGroupWalletSuccessEvent -> onCreateFreeGroupWallet(event)
        }
    }

    private fun onCreateFreeGroupWallet(event: ReviewWalletEvent.CreateFreeGroupWalletSuccessEvent) {
        navigator.openMainScreen(this)
        navigator.openWalletDetailsScreen(this, event.walletId)
    }

    private fun onCreateWalletError(event: CreateWalletErrorEvent) {
        showOrHideLoading(false)
        val message = event.message
        NCToastMessage(this).showWarning(message)
        if (message.isWalletExisted()) {
            navigator.openMainScreen(this)
        }
    }

    private fun onCreateWalletSuccess(event: CreateWalletSuccessEvent) {
        showOrHideLoading(false)
        navigator.openBackupWalletScreen(
            activityContext = this,
            wallet = event.wallet,
            isDecoyWallet = args.decoyPin.isNotEmpty()
        )
    }

    companion object {

        fun start(
            activityContext: Context,
            args: ReviewWalletArgs
        ) {
            activityContext.startActivity(
                Intent(activityContext, ReviewWalletActivity::class.java).apply {
                    putExtras(args.buildBundle())
                }
            )
        }
    }
}

@Composable
fun ReviewWalletContent(
    uiState: ReviewWalletUiState = ReviewWalletUiState(),
    args: ReviewWalletArgs,
    signers: List<SignerModel>,
    onContinue: () -> Unit = {},
    openMainScreen: () -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (args.groupId.isNotEmpty()) {
                        NcHintMessage {
                            Text(
                                text = stringResource(R.string.nc_create_free_group_wallet_hint),
                                style = NunchukTheme.typography.titleSmall
                            )
                        }
                    }
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = onContinue
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            },
            topBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.backgroundMidGray)
                        .systemBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    NcTopAppBar(
                        title = stringResource(id = R.string.nc_wallet_review_wallet_title),
                        textStyle = NunchukTheme.typography.titleLarge,
                        backgroundColor = MaterialTheme.colorScheme.backgroundMidGray,
                    )

                    Text(
                        text = args.walletName,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        style = NunchukTheme.typography.heading
                    )

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            text = if (args.walletType == WalletType.SINGLE_SIG) {
                                stringResource(id = R.string.nc_wallet_single_sig)
                            } else {
                                "${args.totalRequireSigns}/${args.signers.size} ${stringResource(id = R.string.nc_wallet_multisig)}"
                            },
                            style = NunchukTheme.typography.caption
                        )

                        Text(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            text = args.addressType.toReadableString(LocalContext.current),
                            style = NunchukTheme.typography.caption
                        )
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item("key") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NcIcon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.ic_mulitsig_dark),
                            contentDescription = "Key",
                            tint = MaterialTheme.colorScheme.textPrimary
                        )

                        Text(
                            text = stringResource(id = R.string.nc_title_signers),
                            style = NunchukTheme.typography.body
                        )
                    }
                }

                items(signers) {
                    if (it.isVisible) {
                        SignerCard(item = it)
                    } else {
                        UserSignerCard(item = it)
                    }
                }
            }

            if (uiState.groupWalletUnavailable) {
                NcInfoDialog(
                    title = stringResource(id = R.string.nc_unable_access_link),
                    message = stringResource(id = R.string.nc_group_wallet_created_by_others),
                    onPositiveClick = {
                        openMainScreen()
                    },
                    onDismiss = {
                        openMainScreen()
                    },
                    positiveButtonText = stringResource(R.string.nc_return_to_home_screen)
                )
            }
        }
    }
}

@Composable
private fun UserSignerCard(item: SignerModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NcCircleImage(
            resId = R.drawable.ic_user_2,
            color = MaterialTheme.colorScheme.greyLight,
            iconTintColor = MaterialTheme.colorScheme.textPrimary,
        )

        Text(
            text = "XFP: ${item.fingerPrint}",
            style = NunchukTheme.typography.body
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewReviewWalletContent(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    ReviewWalletContent(
        args = ReviewWalletArgs(
            walletName = "Wallet Name",
            walletType = WalletType.SINGLE_SIG,
            addressType = AddressType.LEGACY,
            totalRequireSigns = 1,
            signers = emptyList(),
            groupId = "123"
        ),
        signers = signers
    )
}