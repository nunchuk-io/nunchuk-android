package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.claimnote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.montserratMedium
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.main.R as MainR

@Composable
fun ClaimNoteScreen(
    snackState: SnackbarHostState,
    inheritanceAdditional: InheritanceAdditional,
    modifier: Modifier = Modifier,
    isOnChainClaim: Boolean = false,
    onDoneClick: () -> Unit = {},
    onViewWallet: () -> Unit = {},
    onWithdrawClick: () -> Unit = { },
) {
    ClaimNoteContent(
        modifier = modifier,
        snackState = snackState,
        note = inheritanceAdditional.inheritance?.note.orEmpty(),
        balance = inheritanceAdditional.balance,
        onWithdrawClick = onWithdrawClick,
        onDoneClick = onDoneClick,
        isOnChainClaim = isOnChainClaim,
        onViewWallet = onViewWallet,
    )
}

@Composable
private fun ClaimNoteContent(
    modifier: Modifier = Modifier,
    isOnChainClaim: Boolean = false,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    note: String = "",
    balance: Double = 0.0,
    onWithdrawClick: () -> Unit = {},
    onViewWallet: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {

    NunchukTheme {
        NcScaffold(
            modifier = modifier.navigationBarsPadding(),
            snackState = snackState,
            topBar = {
                NcTopAppBar(
                    backgroundColor = colorResource(id = MainR.color.nc_fill_denim),
                    title = "",
                )
            },
            bottomBar = {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = balance > 0,
                        onClick = onWithdrawClick,
                    ) {
                        Text(text = stringResource(id = R.string.nc_withdraw_bitcoin))
                    }
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .height(48.dp),
                        onClick = {
                            if (isOnChainClaim) {
                                onViewWallet()
                            } else {
                                onDoneClick()
                            }
                        },
                    ) {
                        if (isOnChainClaim) {
                            Text(text = stringResource(MainR.string.nc_view_wallet))
                        } else {
                            Text(text = stringResource(R.string.nc_text_done))
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .background(color = colorResource(id = MainR.color.nc_fill_denim))
                            .height(215.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            text = stringResource(R.string.nc_your_inheritance),
                            style = NunchukTheme.typography.title,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            text = balance.toAmount().getBTCAmount(),
                            style = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = montserratMedium,
                                color = colorResource(
                                    id = MainR.color.nc_text_primary
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            text = balance.toAmount().getCurrencyAmount(),
                            style = NunchukTheme.typography.title,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_congratulation_unlocked_your_inheritance),
                        style = NunchukTheme.typography.heading
                    )
                    if (note.isBlank().not()) {
                        Text(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp
                            ),
                            text = stringResource(R.string.nc_you_have_a_message_below),
                            style = NunchukTheme.typography.body
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                                .fillMaxWidth()
                                .background(
                                    color = colorResource(
                                        id = MainR.color.nc_grey_light
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Text(
                                modifier = Modifier.padding(12.dp),
                                style = NunchukTheme.typography.body,
                                text = note
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ClaimNoteScreenPreview() {
    NunchukTheme {
        ClaimNoteContent(
            note = "This is a sample inheritance note to demonstrate the UI of the Claim Note Screen.",
            balance = 0.01234567,
            onDoneClick = {},
            onWithdrawClick = {}
        )
    }
}

