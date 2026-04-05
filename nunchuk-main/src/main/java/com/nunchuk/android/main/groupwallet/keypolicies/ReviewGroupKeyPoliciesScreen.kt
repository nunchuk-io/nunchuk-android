package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.main.R
import com.nunchuk.android.model.GroupPlatformKeyPolicy
import com.nunchuk.android.model.GroupSpendingLimit
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.type.GroupSpendingLimitInterval
import com.nunchuk.android.type.SignerType

private val ChangedColor = Color(0xFFCF4018)

@Composable
internal fun ReviewGroupKeyPoliciesScreen(
    viewModel: ReviewGroupKeyPoliciesViewModel = hiltViewModel(),
    onBackClicked: () -> Unit = {},
    onOpenWalletAuthentication: (walletId: String, dummyTransactionId: String) -> Unit = { _, _ -> },
    onDiscardSuccess: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ReviewGroupKeyPoliciesEvent.OpenWalletAuthentication -> {
                    onOpenWalletAuthentication(event.walletId, event.dummyTransactionId)
                }

                is ReviewGroupKeyPoliciesEvent.ConfirmDiscard -> onDiscardSuccess()
                is ReviewGroupKeyPoliciesEvent.Error -> {}
            }
        }
    }

    ReviewGroupKeyPoliciesContent(
        state = state,
        onBackClicked = onBackClicked,
        onContinueClicked = viewModel::onContinueClick,
        onConfirmDiscard = viewModel::onConfirmDiscard,
    )
}

@Composable
private fun ReviewGroupKeyPoliciesContent(
    state: ReviewGroupKeyPoliciesUiState = ReviewGroupKeyPoliciesUiState(),
    onBackClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onConfirmDiscard: () -> Unit = {},
) {
    val isGlobalMode = state.policyType == PolicyType.GLOBAL
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }

    NunchukTheme {
        androidx.compose.material3.Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "",
                    onBackPress = onBackClicked,
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onContinueClicked,
                    ) {
                        Text(
                            text = pluralStringResource(
                                id = com.nunchuk.android.core.R.plurals.nc_text_continue_signature_pending,
                                count = state.pendingSignatures,
                                state.pendingSignatures,
                            )
                        )
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showDiscardConfirmation = true },
                    ) {
                        Text(
                            text = stringResource(com.nunchuk.android.core.R.string.nc_discard_changes),
                            style = NunchukTheme.typography.title,
                            color = MaterialTheme.colorScheme.textPrimary,
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.nc_platform_key_policies),
                    style = NunchukTheme.typography.heading,
                )

                ReviewPolicyHeader(
                    isGlobalMode = isGlobalMode,
                    isPolicyTypeChanged = state.oldPolicyType != null && state.policyType != state.oldPolicyType,
                )

                state.newPolicies.forEach { newPolicy ->
                    val oldPolicy = state.oldPolicies.firstOrNull {
                        it.fingerPrint == newPolicy.fingerPrint
                    }
                    val signer = if (isGlobalMode) null else state.signers.firstOrNull {
                        it.fingerPrint == newPolicy.fingerPrint
                    }
                    ReviewPolicyCard(
                        newPolicy = newPolicy,
                        oldPolicy = oldPolicy,
                        signer = signer,
                        isGlobalMode = isGlobalMode,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showDiscardConfirmation) {
            NcConfirmationDialog(
                title = stringResource(com.nunchuk.android.core.R.string.nc_confirmation),
                message = stringResource(com.nunchuk.android.core.R.string.nc_are_you_sure_discard_the_change),
                onPositiveClick = {
                    showDiscardConfirmation = false
                    onConfirmDiscard()
                },
                onDismiss = { showDiscardConfirmation = false },
            )
        }

        if (state.isLoading) {
            NcLoadingDialog()
        }
    }
}

@Composable
private fun ReviewPolicyHeader(
    isGlobalMode: Boolean,
    isPolicyTypeChanged: Boolean = false,
) {
    Text(
        modifier = Modifier.padding(top = 24.dp),
        text = if (isGlobalMode) {
            stringResource(R.string.nc_global_policy)
        } else {
            stringResource(R.string.nc_per_key_policy)
        },
        style = NunchukTheme.typography.title.copy(
            color = if (isPolicyTypeChanged) ChangedColor else Color.Unspecified,
        ),
    )

    Text(
        modifier = Modifier.padding(top = 8.dp),
        text = if (isGlobalMode) {
            stringResource(R.string.nc_global_policy_desc)
        } else {
            stringResource(R.string.nc_per_key_policy_desc)
        },
        style = NunchukTheme.typography.bodySmall.copy(
            color = if (isPolicyTypeChanged) ChangedColor else MaterialTheme.colorScheme.textSecondary
        ),
    )

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.strokePrimary,
    )
}

@Composable
private fun ReviewPolicyCard(
    modifier: Modifier = Modifier,
    newPolicy: KeyPolicyItem,
    oldPolicy: KeyPolicyItem?,
    signer: SignerModel?,
    isGlobalMode: Boolean,
) {
    val normalizedNew = normalizeGroupPlatformKeyPolicy(newPolicy.keyPolicy)
    val normalizedOld = oldPolicy?.let { normalizeGroupPlatformKeyPolicy(it.keyPolicy) }

    val isSpendingLimitChanged = normalizedOld != null &&
            normalizedNew.spendingLimit != normalizedOld.spendingLimit
    val isDelayChanged = normalizedOld != null &&
            normalizedNew.signingDelaySeconds != normalizedOld.signingDelaySeconds
    val isAutoBroadcastChanged = normalizedOld != null &&
            normalizedNew.autoBroadcastTransaction != normalizedOld.autoBroadcastTransaction

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.strokePrimary,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isGlobalMode) {
                NcCircleImage(
                    resId = R.drawable.ic_policy_keys,
                    size = 36.dp,
                    iconSize = 24.dp,
                )
            } else if (signer != null) {
                NcCircleImage(
                    resId = signer.toReadableDrawableResId(),
                    size = 36.dp,
                    iconSize = 24.dp,
                    color = MaterialTheme.colorScheme.greyLight,
                    iconTintColor = MaterialTheme.colorScheme.textPrimary,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (isGlobalMode) {
                        stringResource(R.string.nc_all_keys)
                    } else {
                        buildSignerLabel(signer)
                    },
                    style = NunchukTheme.typography.bodySmall,
                )
                Text(
                    text = formatGroupSpendingLimitOrUnlimited(normalizedNew.spendingLimit),
                    style = NunchukTheme.typography.title.copy(
                        color = if (isSpendingLimitChanged) ChangedColor else Color.Unspecified
                    ),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.strokePrimary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(com.nunchuk.android.core.R.string.nc_enable_co_signing_delay),
                style = NunchukTheme.typography.body,
            )
            Text(
                text = if (normalizedNew.signingDelaySeconds > 0) {
                    formatCoSigningDelayText(
                        normalizedNew.signingDelaySeconds / KeyPolicy.ONE_HOUR_TO_SECONDS,
                        (normalizedNew.signingDelaySeconds % KeyPolicy.ONE_HOUR_TO_SECONDS) / KeyPolicy.ONE_MINUTE_TO_SECONDS
                    )
                } else {
                    stringResource(com.nunchuk.android.core.R.string.nc_off)
                },
                style = NunchukTheme.typography.title.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDelayChanged) ChangedColor else Color.Unspecified,
                ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.nc_auto_broadcast),
                style = NunchukTheme.typography.body,
            )
            Text(
                text = if (normalizedNew.autoBroadcastTransaction) {
                    stringResource(com.nunchuk.android.core.R.string.nc_on)
                } else {
                    stringResource(com.nunchuk.android.core.R.string.nc_off)
                },
                style = NunchukTheme.typography.title.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isAutoBroadcastChanged) ChangedColor else Color.Unspecified,
                ),
            )
        }
    }
}

@Composable
private fun formatCoSigningDelayText(hours: Int, minutes: Int): String {
    return when {
        hours == 0 && minutes == 0 -> stringResource(com.nunchuk.android.core.R.string.nc_off)
        hours > 0 && minutes > 0 -> "$hours hours $minutes min"
        hours > 0 -> "$hours hours"
        else -> "$minutes min"
    }
}

@PreviewLightDark
@Composable
private fun ReviewGroupKeyPoliciesGlobalPreview() {
    ReviewGroupKeyPoliciesContent(
        state = ReviewGroupKeyPoliciesUiState(
            policyType = PolicyType.GLOBAL,
            pendingSignatures = 2,
            newPolicies = listOf(
                KeyPolicyItem(
                    keyPolicy = GroupPlatformKeyPolicy(
                        spendingLimit = GroupSpendingLimit(
                            amount = "5000",
                            interval = GroupSpendingLimitInterval.DAILY,
                            currency = "USD",
                        ),
                        autoBroadcastTransaction = true,
                    ),
                ),
            ),
            oldPolicies = listOf(
                KeyPolicyItem(
                    keyPolicy = GroupPlatformKeyPolicy(
                        spendingLimit = GroupSpendingLimit(
                            amount = "1000",
                            interval = GroupSpendingLimitInterval.DAILY,
                            currency = "USD",
                        ),
                        autoBroadcastTransaction = false,
                    ),
                ),
            ),
        ),
    )
}

@PreviewLightDark
@Composable
private fun ReviewGroupKeyPoliciesPerKeyPreview() {
    val signer1 = SignerModel(
        id = "1",
        name = "TAPSIGNER",
        derivationPath = "m/48'/0'/0'/2'",
        fingerPrint = "B35F4A00",
        type = SignerType.NFC,
        cardId = "00000B35F4",
        isMasterSigner = false,
    )
    val signer2 = SignerModel(
        id = "2",
        name = "Trezor",
        derivationPath = "m/48'/0'/0'/2'",
        fingerPrint = "79EB35F4",
        type = SignerType.HARDWARE,
        isMasterSigner = false,
    )
    ReviewGroupKeyPoliciesContent(
        state = ReviewGroupKeyPoliciesUiState(
            policyType = PolicyType.PER_KEY,
            signers = listOf(signer1, signer2),
            pendingSignatures = 2,
            newPolicies = listOf(
                KeyPolicyItem(fingerPrint = "B35F4A00",
                    keyPolicy = GroupPlatformKeyPolicy(
                        autoBroadcastTransaction = true,
                    ),
                ),
                KeyPolicyItem(
                    fingerPrint = "79EB35F4",
                    keyPolicy = GroupPlatformKeyPolicy(
                        spendingLimit = GroupSpendingLimit(
                            amount = "100",
                            interval = GroupSpendingLimitInterval.DAILY,
                            currency = "USD",
                        ),
                        signingDelaySeconds = 7200,
                        autoBroadcastTransaction = true,
                    ),
                ),
            ),
            oldPolicies = listOf(
                KeyPolicyItem(fingerPrint = "B35F4A00"),
                KeyPolicyItem(
                    fingerPrint = "79EB35F4",
                    keyPolicy = GroupPlatformKeyPolicy(
                        spendingLimit = GroupSpendingLimit(
                            amount = "50",
                            interval = GroupSpendingLimitInterval.DAILY,
                            currency = "USD",
                        ),
                        signingDelaySeconds = 3600,
                        autoBroadcastTransaction = false,
                    ),
                ),
            ),
        ),
    )
}
