package com.nunchuk.android.main.membership.onchaintimelock.addkey

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcSecondaryButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.type.SignerType

/** The red "Inheritance" tag next to the Primary inheritance heading (SET-02). */
@Composable
internal fun InheritanceBadge(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(id = R.drawable.ic_badge_inheritance),
        contentDescription = stringResource(R.string.nc_membership_cd_inheritance_icon),
    )
}

/**
 * A before-the-timelock card of a configurable draft: one device, one chip per account the policy
 * uses (SET-02: Acct X / Acct Y; SET-16: only Acct 1 once the primary is customized).
 */
@Composable
internal fun DraftSpendingCard(
    card: DraftSetupCard,
    number: Int,
    isDisabled: Boolean = false,
    onAddClicked: (DraftSetupCard, DraftSetupSlot) -> Unit = { _, _ -> },
) {
    val device = card.device
    val nextSlot = card.nextEmptySlot
    when {
        device == null -> NcDashLineBox {
            DraftKeyRow(
                title = stringResource(R.string.nc_hardware_key_number, number),
                signer = null,
                accountChips = card.slots.mapIndexed { i, slot -> accountChip(slot, i) },
            ) {
                AddButton(enabled = !isDisabled && nextSlot != null) { nextSlot?.let { onAddClicked(card, it) } }
            }
        }

        nextSlot != null -> NcDashLineBox(showDashedBorder = true) {
            Box(Modifier.background(colorResource(R.color.nc_fill_slime).copy(alpha = 0.4f), RoundedCornerShape(8.dp))) {
                DraftKeyRow(
                    title = device.name,
                    signer = device,
                    accountChips = card.slots.mapIndexed { i, slot -> accountChip(slot, i) },
                ) {
                    AddButton(enabled = !isDisabled) { onAddClicked(card, nextSlot) }
                }
            }
        }

        else -> Box(Modifier.background(colorResource(R.color.nc_fill_slime), RoundedCornerShape(8.dp))) {
            DraftKeyRow(
                title = device.name,
                signer = device,
                accountChips = card.slots.mapIndexed { i, slot -> accountChip(slot, i) },
            ) {
                CheckedLabel(stringResource(R.string.nc_added))
            }
        }
    }
}

/**
 * One primary-inheritance slot (SET-02 / SET-15 / SET-08b / KEY-08 / PRI-07). Empty: Add. Filled:
 * Verify until its backup is checked (amber), then green; a key a validation error names is circled
 * in red, and a flagged customized slot offers Change (PRI-07). The guided default's inheritance key
 * keeps Verify while [isVerificationPending] — it is the way into Verify backups, where the owner
 * key the Beneficiary gets is verified too.
 */
@Composable
internal fun DraftPrimarySlotCard(
    card: DraftSetupCard,
    slot: DraftSetupSlot,
    number: Int,
    isCustomized: Boolean,
    isHighlighted: Boolean,
    isVerificationPending: Boolean = false,
    /** Every primary key is verified: a customized slot drops its Verified label (SET-16). */
    isPrimaryVerified: Boolean = false,
    /** A fallback slot: a verified key shows "Added" (FBK-10 / SET-11). */
    isFallback: Boolean = false,
    isDisabled: Boolean = false,
    onAddClicked: (DraftSetupCard, DraftSetupSlot) -> Unit = { _, _ -> },
    onVerifyClicked: (DraftSetupCard, DraftSetupSlot) -> Unit = { _, _ -> },
) {
    val signer = slot.signer
    val errorBorder = if (isHighlighted) {
        Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.error), RoundedCornerShape(8.dp))
    } else {
        Modifier
    }
    if (signer == null) {
        NcDashLineBox(modifier = errorBorder) {
            DraftKeyRow(
                title = if (isCustomized) {
                    stringResource(R.string.nc_hardware_key_number, number)
                } else {
                    stringResource(R.string.nc_inheritance_key)
                },
                signer = null,
                accountChips = listOf(accountChip(slot, 0)),
            ) {
                AddButton(enabled = !isDisabled) { onAddClicked(card, slot) }
            }
        }
        return
    }
    val background = when {
        isHighlighted -> MaterialTheme.colorScheme.background
        slot.isVerified -> colorResource(R.color.nc_fill_slime)
        else -> colorResource(R.color.nc_fill_beewax)
    }
    Box(
        modifier = errorBorder
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
    ) {
        DraftKeyRow(
            title = signer.name,
            signer = signer,
            accountChips = listOf(accountChip(slot, 0)),
        ) {
            when {
                isHighlighted && isCustomized -> SmallOutlineButton(
                    text = stringResource(R.string.nc_change),
                    enabled = !isDisabled,
                ) { onAddClicked(card, slot) }

                !slot.isVerified || (!isCustomized && isVerificationPending) -> SmallOutlineButton(
                    text = stringResource(R.string.nc_verify),
                    enabled = !isDisabled,
                ) { onVerifyClicked(card, slot) }

                isCustomized && !isFallback -> if (!isPrimaryVerified) CheckedLabel(stringResource(R.string.nc_verified))
                else -> CheckedLabel(stringResource(R.string.nc_added))
            }
        }
    }
}

private data class AccountChip(val label: String, val isPlaceholder: Boolean)

@Composable
private fun accountChip(slot: DraftSetupSlot, position: Int): AccountChip {
    val account = slot.signer?.index ?: slot.accountIndex
    return if (slot.signer != null && account != null) {
        AccountChip(stringResource(R.string.nc_acct_x, account), isPlaceholder = false)
    } else {
        AccountChip(
            stringResource(
                if (position == 0) R.string.nc_membership_acct_x_placeholder else R.string.nc_membership_acct_y_placeholder
            ),
            isPlaceholder = true,
        )
    }
}

@Composable
private fun DraftKeyRow(
    title: String,
    signer: SignerModel?,
    accountChips: List<AccountChip>,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcCircleImage(resId = signer?.toReadableDrawableResId() ?: R.drawable.ic_hardware_key)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(text = title, style = NunchukTheme.typography.body)
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                signer?.let {
                    NcTag(
                        label = it.toReadableSignerType(context = LocalContext.current),
                        backgroundColor = colorResource(id = R.color.nc_bg_mid_gray),
                    )
                }
                accountChips.forEach { chip ->
                    NcTag(
                        modifier = Modifier.alpha(if (chip.isPlaceholder) 0.5f else 1f),
                        label = chip.label,
                        backgroundColor = colorResource(id = R.color.nc_bg_mid_gray),
                    )
                }
            }
            signer?.let {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = it.getXfpOrCardIdLabel(),
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    SmallOutlineButton(text = stringResource(R.string.nc_add), enabled = enabled, onClick = onClick)
}

@Composable
private fun SmallOutlineButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    NcOutlineButton(modifier = Modifier.height(36.dp), enabled = enabled, onClick = onClick) {
        Text(text = text, style = NunchukTheme.typography.caption)
    }
}

@Composable
private fun CheckedLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.nc_circle_checked),
            contentDescription = stringResource(R.string.nc_membership_cd_checked_icon),
        )
        Text(modifier = Modifier.padding(start = 4.dp), text = text, style = NunchukTheme.typography.body)
    }
}

/**
 * KEY-01a "Choose a key": a key already in this setup, or the regular add-key flow. [fallbackNumber]
 * switches to the fallback wording ("Choose a key for Fallback 1").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChooseKeySheet(
    options: List<SignerModel>,
    fallbackNumber: Int? = null,
    onChooseKey: (xfp: String) -> Unit,
    onAddKey: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
    ) {
        ChooseKeyContent(
            options = options,
            fallbackNumber = fallbackNumber,
            onChooseKey = onChooseKey,
            onAddKey = onAddKey,
        )
    }
}

@Composable
private fun ChooseKeyContent(
    options: List<SignerModel>,
    fallbackNumber: Int? = null,
    onChooseKey: (xfp: String) -> Unit = {},
    onAddKey: () -> Unit = {},
) {
    var selectedXfp by rememberSaveable { mutableStateOf(options.firstOrNull()?.fingerPrint) }
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = if (fallbackNumber != null) {
                stringResource(R.string.nc_choose_key_for_fallback, fallbackNumber)
            } else {
                stringResource(R.string.nc_choose_a_key)
            },
            style = NunchukTheme.typography.title,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            text = stringResource(
                if (fallbackNumber != null) R.string.nc_choose_key_for_fallback_desc else R.string.nc_choose_a_key_desc
            ),
            style = NunchukTheme.typography.body,
        )
        options.forEach { signer ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedXfp = signer.fingerPrint }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NcCircleImage(resId = signer.toReadableDrawableResId())
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(text = signer.name, style = NunchukTheme.typography.body)
                    NcTag(
                        modifier = Modifier.padding(top = 4.dp),
                        label = signer.toReadableSignerType(context = LocalContext.current),
                        backgroundColor = colorResource(id = R.color.nc_bg_mid_gray),
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = signer.getXfpOrCardIdLabel(),
                        style = NunchukTheme.typography.bodySmall,
                    )
                }
                NcRadioButton(
                    selected = selectedXfp == signer.fingerPrint,
                    onClick = { selectedXfp = signer.fingerPrint },
                )
            }
        }
        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            enabled = selectedXfp != null,
            onClick = { selectedXfp?.let(onChooseKey) },
        ) {
            Text(text = stringResource(R.string.nc_choose_key))
        }
        NcSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            onClick = onAddKey,
        ) {
            Text(
                text = stringResource(if (fallbackNumber != null) R.string.nc_use_a_different_key else R.string.nc_add_key)
            )
        }
    }
}

/**
 * VER-01 / VER-01a / VER-02 "Verify backups" of the guided default: the inheritance key is
 * required, plus at least one owner key — the one shared with the Beneficiary.
 */
@Composable
internal fun VerifyBackupsScreen(
    remainingTime: Int,
    inheritanceKeys: List<DraftSetupSlot>,
    ownerKeys: List<DraftSetupSlot>,
    /** Every account of each owner device in this setup, by XFP (VER-02: Acct 0 / Acct 1). */
    ownerAccounts: Map<String, List<Int>> = emptyMap(),
    onVerifyClicked: (SignerModel) -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    BackHandler(onBack = onDone)
    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = if (remainingTime <= 0) "" else stringResource(R.string.nc_estimate_remain_time, remainingTime),
                onBackPress = onDone,
                actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = stringResource(R.string.nc_membership_cd_more_icon)
                        )
                    }
                },
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onDone,
            ) {
                Text(text = stringResource(R.string.nc_text_done))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.nc_verify_backups),
                style = NunchukTheme.typography.heading,
            )
            Text(text = stringResource(R.string.nc_verify_backups_desc), style = NunchukTheme.typography.body)
            Text(
                text = stringResource(R.string.nc_verify_backups_inheritance_key_required),
                style = NunchukTheme.typography.title,
            )
            inheritanceKeys.forEach { VerifyBackupRow(it, onVerifyClicked = onVerifyClicked) }
            Text(text = stringResource(R.string.nc_verify_backups_other_keys), style = NunchukTheme.typography.title)
            Text(
                text = stringResource(
                    if (ownerKeys.isEmpty()) R.string.nc_verify_backups_no_other_keys else R.string.nc_verify_backups_other_keys_desc
                ),
                style = NunchukTheme.typography.body,
                color = MaterialTheme.colorScheme.textSecondary,
            )
            ownerKeys.forEach { slot ->
                VerifyBackupRow(slot, ownerAccounts[slot.signer?.fingerPrint?.lowercase()].orEmpty(), onVerifyClicked)
            }
        }
    }
}

@Composable
private fun VerifyBackupRow(
    slot: DraftSetupSlot,
    accounts: List<Int> = emptyList(),
    onVerifyClicked: (SignerModel) -> Unit,
) {
    val signer = slot.signer ?: return
    DraftKeyRow(
        title = signer.name,
        signer = signer,
        accountChips = accounts.ifEmpty { listOf(signer.index) }.map {
            AccountChip(stringResource(R.string.nc_acct_x, it), isPlaceholder = false)
        },
    ) {
        if (slot.isVerified) {
            CheckedLabel(stringResource(R.string.nc_verified))
        } else {
            SmallOutlineButton(text = stringResource(R.string.nc_verify), enabled = true) { onVerifyClicked(signer) }
        }
    }
}

private val previewSigner = SignerModel(
    id = "79eb35f4",
    name = "Keystone",
    derivationPath = "m/48h/0h/0h/2h",
    fingerPrint = "79eb35f4",
    type = SignerType.AIRGAP,
    isMasterSigner = false,
)

private val previewSpendingCard = DraftSetupCard(
    card = DraftInheritanceCard(
        section = InheritanceSection.SPENDING,
        fallbackIndex = null,
        isServerKey = false,
        tags = emptyList(),
        keys = emptyList(),
        accountIndexByXfp = emptyMap(),
    ),
    slots = listOf(
        DraftSetupSlot(0, 0, InheritanceSection.PRIMARY, previewSigner, VerifyType.APP_VERIFIED),
        DraftSetupSlot(1, 1, InheritanceSection.SPENDING),
    ),
)

@PreviewLightDark
@Composable
private fun DraftSetupCardsPreview() {
    NunchukTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DraftSpendingCard(card = previewSpendingCard.copy(slots = previewSpendingCard.slots.map { it.copy(signer = null) }), number = 1)
            DraftSpendingCard(card = previewSpendingCard, number = 2)
            DraftPrimarySlotCard(
                card = previewSpendingCard,
                slot = DraftSetupSlot(7, 0, InheritanceSection.PRIMARY, previewSigner.copy(name = "Jade 2")),
                number = 1,
                isCustomized = false,
                isHighlighted = false,
            )
            DraftPrimarySlotCard(
                card = previewSpendingCard,
                slot = DraftSetupSlot(7, 0, InheritanceSection.PRIMARY, previewSigner),
                number = 1,
                isCustomized = true,
                isHighlighted = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ChooseKeyContentPreview() {
    NunchukTheme {
        ChooseKeyContent(options = listOf(previewSigner, previewSigner.copy(fingerPrint = "a47c3e19", name = "COLDCARD")))
    }
}

@PreviewLightDark
@Composable
private fun VerifyBackupsScreenPreview() {
    NunchukTheme {
        VerifyBackupsScreen(
            remainingTime = 10,
            inheritanceKeys = listOf(DraftSetupSlot(7, 0, InheritanceSection.PRIMARY, previewSigner.copy(name = "Jade 2"))),
            ownerKeys = listOf(DraftSetupSlot(0, 0, InheritanceSection.PRIMARY, previewSigner, VerifyType.APP_VERIFIED)),
        )
    }
}
