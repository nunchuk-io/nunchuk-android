package com.nunchuk.android.main.membership.byzantine.addKey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.pullrefresh.PullRefreshIndicator
import com.nunchuk.android.compose.pullrefresh.pullRefresh
import com.nunchuk.android.compose.pullrefresh.rememberPullRefreshState
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.AddKeyCard
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.byzantine.isFacilitatorAdmin
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

@Composable
fun AddByzantineKeyListContent(
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    refresh: () -> Unit = { },
    onMoreClicked: () -> Unit = {},
    keys: List<AddKeyData> = emptyList(),
    missingBackupKeys: List<AddKeyData> = emptyList(),
    remainingTime: Int,
    isRefreshing: Boolean = false,
    isAddOnly: Boolean = false,
    groupWalletType: GroupWalletType? = null,
    role: AssistedWalletRole = AssistedWalletRole.NONE,
) {
    val state = rememberPullRefreshState(isRefreshing, refresh)
    val continueButtonEnabled = remember(keys) {
        keys.all { it.isVerifyOrAddKey }
                && (missingBackupKeys.isEmpty() || keys.filter {
            it.signer?.type != SignerType.NFC && it.signer?.tags?.contains(
                SignerTag.INHERITANCE
            ) == true
        }.all { it.verifyType != VerifyType.NONE })
    }
    NunchukTheme {
        Scaffold(modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    })
            }, bottomBar = {
                if (isAddOnly.not()) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClicked,
                        enabled = continueButtonEnabled
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            }) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pullRefresh(state)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.nc_let_add_your_keys),
                            style = NunchukTheme.typography.heading
                        )
                        val description = when (groupWalletType) {
                            GroupWalletType.TWO_OF_FOUR_MULTISIG -> stringResource(R.string.nc_byzantine_add_key_2_of_4_desc)
                            GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE -> stringResource(R.string.nc_byzantine_add_key_2_of_4_no_inheritance_desc)
                            GroupWalletType.TWO_OF_THREE -> stringResource(R.string.nc_byzantine_add_key_2_of_3_desc)
                            GroupWalletType.THREE_OF_FIVE, GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY -> stringResource(
                                R.string.nc_byzantine_add_key_3_of_5_desc
                            )

                            GroupWalletType.THREE_OF_FIVE_INHERITANCE -> stringResource(R.string.nc_byzantine_add_key_3_of_5_inheritance_desc)
                            GroupWalletType.TWO_OF_THREE_PLATFORM_KEY, null -> ""
                        }
                        NcSpannedText(
                            modifier = Modifier
                                .padding(top = 16.dp, bottom = 4.dp)
                                .padding(horizontal = 16.dp),
                            text = description,
                            baseStyle = NunchukTheme.typography.body,
                            styles = mapOf(SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold))
                        )
                    }

                    items(keys) { key ->
                        BlurView(
                            isBlur = (key.signer?.isVisible == false || key.type == MembershipStep.ADD_SEVER_KEY) && role == AssistedWalletRole.KEYHOLDER_LIMITED,
                        ) { modifier ->
                            AddKeyCard(
                                modifier = modifier,
                                item = key,
                                onAddClicked = onAddClicked,
                                onVerifyClicked = onVerifyClicked,
                                isDisabled = role.isFacilitatorAdmin,
                                isMissingBackup = missingBackupKeys.contains(key) && key.signer?.type != SignerType.NFC,
                            )
                        }
                    }
                }

                PullRefreshIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun BlurView(
    isBlur: Boolean,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    if (isBlur) {
        content(Modifier
            .graphicsLayer {
                renderEffect = BlurEffect(16f, 16f, TileMode.Decal)
            }
            .padding(horizontal = 16.dp))
    } else {
        content(Modifier.padding(horizontal = 16.dp))
    }
}

@Preview
@Composable
fun AddKeyListScreenHoneyBadgerPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    AddByzantineKeyListContent(
        keys = listOf(
            AddKeyData(
                type = MembershipStep.HONEY_ADD_INHERITANCE_KEY,
                signer = signer.copy(isVisible = false),
                verifyType = VerifyType.NONE
            ),
            AddKeyData(
                type = MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
                signer = signer,
                verifyType = VerifyType.NONE
            ),
            AddKeyData(
                type = MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
            ),
            AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
        ),
        remainingTime = 0,
        role = AssistedWalletRole.KEYHOLDER_LIMITED,
        groupWalletType = GroupWalletType.TWO_OF_FOUR_MULTISIG,
    )
}