package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSelectableBottomSheet
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
import com.nunchuk.android.model.GroupDummyTransaction
import com.nunchuk.android.model.GroupPlatformKeyPolicies
import com.nunchuk.android.model.GroupPlatformKeyPolicy
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.GroupSpendingLimit
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.type.GroupSpendingLimitInterval
import com.nunchuk.android.type.SignerType

private const val GLOBAL_POLICY_EDIT_KEY = "__global_policy__"

@Composable
internal fun FreeGroupKeyPoliciesScreen(
    groupId: String = "",
    walletId: String = "",
    allSigners: List<SignerModel> = emptyList(),
    platformKeyPolicies: GroupPlatformKeyPolicies? = null,
    onBackClicked: () -> Unit = {},
    onSaveSuccess: (GroupSandbox) -> Unit = {},
    onUpdatePolicySuccess: () -> Unit = {},
    onOpenWalletAuthentication: (walletId: String, dummyTransaction: GroupDummyTransaction?) -> Unit = { _, _ -> },
) {
    val viewModel =
        hiltViewModel<FreeGroupKeyPoliciesViewModel, FreeGroupKeyPoliciesViewModel.Factory> { factory ->
            factory.create(
                groupId = groupId,
                walletId = walletId,
                allSigners = allSigners,
                platformKeyPolicies = platformKeyPolicies,
            )
        }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is FreeGroupKeyPoliciesEvent.SaveSuccess -> onSaveSuccess(event.groupSandbox)
                is FreeGroupKeyPoliciesEvent.UpdatePolicySuccess -> onUpdatePolicySuccess()
                is FreeGroupKeyPoliciesEvent.Error -> {}
                is FreeGroupKeyPoliciesEvent.OpenWalletAuthentication -> {
                    onOpenWalletAuthentication(event.walletId, event.dummyTransaction)
                }
            }
        }
    }

    FreeGroupKeyPoliciesContent(
        state = state,
        isCreatingWallet = walletId.isEmpty(),
        onBackClicked = onBackClicked,
        onChangePolicyType = viewModel::changePolicyType,
        onUpdatePolicy = viewModel::updatePolicy,
        onApplyClicked = viewModel::applyChanges,
        onRemovePlatformKey = viewModel::disablePlatformKey,
        onDismissPreviewWarning = viewModel::dismissPreviewWarning,
        onConfirmApplyChanges = viewModel::confirmApplyChanges,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FreeGroupKeyPoliciesContent(
    state: FreeGroupKeyPoliciesUiState = FreeGroupKeyPoliciesUiState(),
    isCreatingWallet: Boolean = false,
    onBackClicked: () -> Unit = {},
    onChangePolicyType: (PolicyType) -> Unit = {},
    onUpdatePolicy: (KeyPolicyItem) -> Unit = {},
    onApplyClicked: () -> Unit = {},
    onRemovePlatformKey: () -> Unit = {},
    onDismissPreviewWarning: () -> Unit = {},
    onConfirmApplyChanges: () -> Unit = {},
) {
    var showPolicyTypeBottomSheet by rememberSaveable { mutableStateOf(false) }
    var editingPolicyKey by rememberSaveable { mutableStateOf("") }
    var showMoreOption by rememberSaveable { mutableStateOf(false) }
    var showRemoveConfirmation by rememberSaveable { mutableStateOf(false) }

    val isGlobalMode = state.policyType == PolicyType.GLOBAL

    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "",
                    onBackPress = onBackClicked,
                    actions = {
                        if (isCreatingWallet) {
                            IconButton(onClick = { showMoreOption = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More",
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.hasChanges,
                        onClick = onApplyClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_continue_save_changes))
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

                PolicyHeader(
                    isGlobalMode = isGlobalMode,
                    onEditPolicyType = { showPolicyTypeBottomSheet = true },
                )

                state.policies.forEach { policy ->
                    val signer = if (isGlobalMode) null else state.signers.firstOrNull {
                        it.fingerPrint == policy.fingerPrint
                    }
                    val policyKey = if (isGlobalMode) GLOBAL_POLICY_EDIT_KEY else policy.fingerPrint
                    PolicyCard(
                        policy = policy,
                        signer = signer,
                        isGlobalMode = isGlobalMode,
                        onEditSpendingLimit = { editingPolicyKey = policyKey },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (editingPolicyKey.isNotEmpty()) {
            val policy = if (isGlobalMode) {
                state.policies.firstOrNull()
            } else {
                state.policies.firstOrNull { it.fingerPrint == editingPolicyKey }
            }
            val signer = if (isGlobalMode) null else state.signers.firstOrNull {
                it.fingerPrint == editingPolicyKey
            }
            if (policy != null) {
                EditGlobalPolicyBottomSheet(
                    policy = policy,
                    signer = signer,
                    isGlobalMode = isGlobalMode,
                    onDismiss = { editingPolicyKey = "" },
                    onSave = { updatedPolicy ->
                        onUpdatePolicy(updatedPolicy)
                        editingPolicyKey = ""
                    },
                )
            }
        }

        if (showPolicyTypeBottomSheet) {
            PolicyTypeBottomSheet(
                onSelected = { type ->
                    showPolicyTypeBottomSheet = false
                    if (type != state.policyType) {
                        onChangePolicyType(type)
                    }
                },
                onDismiss = { showPolicyTypeBottomSheet = false },
            )
        }

        if (showMoreOption) {
            NcSelectableBottomSheet(
                options = listOf(stringResource(R.string.nc_remove_platform_key)),
                onSelected = {
                    showMoreOption = false
                    showRemoveConfirmation = true
                },
                onDismiss = { showMoreOption = false },
            )
        }

        if (showRemoveConfirmation) {
            NcConfirmationDialog(
                title = stringResource(R.string.nc_remove_platform_key),
                message = stringResource(R.string.nc_remove_platform_key_desc),
                positiveButtonText = stringResource(R.string.nc_remove),
                negativeButtonText = stringResource(com.nunchuk.android.core.R.string.nc_cancel),
                onPositiveClick = {
                    showRemoveConfirmation = false
                    onRemovePlatformKey()
                },
                onDismiss = { showRemoveConfirmation = false },
            )
        }

        if (state.previewWarning != null) {
            val delaySeconds = state.previewWarning.delayApplyInSeconds
            val message = if (delaySeconds > 0) {
                val hours = delaySeconds / 3600
                stringResource(
                    R.string.nc_warning_dummy_transaction_with_delay,
                    hours
                )
            } else {
                stringResource(R.string.nc_warning_dummy_transaction)
            }
            NcConfirmationDialog(
                title = stringResource(com.nunchuk.android.widget.R.string.nc_text_warning),
                message = message,
                positiveButtonText = stringResource(com.nunchuk.android.core.R.string.nc_sign),
                negativeButtonText = stringResource(com.nunchuk.android.core.R.string.nc_cancel),
                onPositiveClick = onConfirmApplyChanges,
                onDismiss = onDismissPreviewWarning,
            )
        }

        if (state.isLoading) {
            NcLoadingDialog()
        }
    }
}

@Composable
private fun PolicyHeader(
    isGlobalMode: Boolean,
    onEditPolicyType: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isGlobalMode) {
                stringResource(R.string.nc_global_policy)
            } else {
                stringResource(R.string.nc_per_key_policy)
            },
            style = NunchukTheme.typography.title,
        )
        Text(
            modifier = Modifier.clickable(onClick = onEditPolicyType),
            text = stringResource(com.nunchuk.android.core.R.string.nc_edit),
            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline),
        )
    }

    Text(
        modifier = Modifier.padding(top = 8.dp),
        text = if (isGlobalMode) {
            stringResource(R.string.nc_global_policy_desc)
        } else {
            stringResource(R.string.nc_per_key_policy_desc)
        },
        style = NunchukTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.textSecondary
        ),
    )

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.strokePrimary,
    )
}

@Composable
private fun PolicyCard(
    modifier: Modifier = Modifier,
    policy: KeyPolicyItem,
    signer: SignerModel?,
    isGlobalMode: Boolean,
    onEditSpendingLimit: () -> Unit = {},
) {
    val normalizedPolicy = normalizeGroupPlatformKeyPolicy(policy.keyPolicy)

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
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
                        text = formatGroupSpendingLimit(
                            normalizedPolicy.spendingLimit ?: defaultGroupSpendingLimit()
                        ),
                        style = NunchukTheme.typography.title,
                    )
                }
            }
            Text(
                modifier = Modifier.clickable(onClick = onEditSpendingLimit),
                text = stringResource(com.nunchuk.android.core.R.string.nc_edit),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline),
            )
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
                text = if (normalizedPolicy.signingDelaySeconds > 0) {
                    formatCoSigningDelay(
                        normalizedPolicy.signingDelaySeconds / KeyPolicy.ONE_HOUR_TO_SECONDS,
                        (normalizedPolicy.signingDelaySeconds % KeyPolicy.ONE_HOUR_TO_SECONDS) / KeyPolicy.ONE_MINUTE_TO_SECONDS
                    )
                } else {
                    stringResource(com.nunchuk.android.core.R.string.nc_off)
                },
                style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold),
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
                text = if (normalizedPolicy.autoBroadcastTransaction) {
                    stringResource(com.nunchuk.android.core.R.string.nc_on)
                } else {
                    stringResource(com.nunchuk.android.core.R.string.nc_off)
                },
                style = NunchukTheme.typography.title.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun formatCoSigningDelay(hours: Int, minutes: Int): String {
    return when {
        hours == 0 && minutes == 0 -> stringResource(com.nunchuk.android.core.R.string.nc_off)
        hours > 0 && minutes > 0 -> "$hours hours $minutes min"
        hours > 0 -> "$hours hours"
        else -> "$minutes min"
    }
}

@PreviewLightDark
@Composable
private fun FreeGroupKeyPoliciesGlobalPreview() {
    FreeGroupKeyPoliciesContent()
}

@PreviewLightDark
@Composable
private fun FreeGroupKeyPoliciesPerKeyPreview() {
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
    val signer3 = SignerModel(
        id = "3",
        name = "TAPSIGNER",
        derivationPath = "m/48'/0'/0'/2'",
        fingerPrint = "B35F4A01",
        type = SignerType.NFC,
        cardId = "00000B35F4",
        isMasterSigner = false,
    )
    FreeGroupKeyPoliciesContent(
        state = FreeGroupKeyPoliciesUiState(
            policyType = PolicyType.PER_KEY,
            signers = listOf(signer1, signer2, signer3),
            policies = listOf(
                KeyPolicyItem(fingerPrint = "B35F4A00"),
                KeyPolicyItem(
                    fingerPrint = "79EB35F4",
                    keyPolicy = GroupPlatformKeyPolicy(
                        spendingLimit = GroupSpendingLimit(
                            amount = "100",
                            interval = GroupSpendingLimitInterval.DAILY,
                            currency = "USD",
                        ),
                        autoBroadcastTransaction = true,
                    ),
                ),
                KeyPolicyItem(fingerPrint = "B35F4A01"),
            ),
            hasChanges = true,
        ),
    )
}
