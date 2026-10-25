package com.nunchuk.android.main.membership.onchaintimelock.replacekey

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.onchaintimelock.addkey.InheritanceValidationErrors
import com.nunchuk.android.main.membership.onchaintimelock.addkey.PrimaryInheritanceHeader
import com.nunchuk.android.main.membership.onchaintimelock.addkey.afterTimelockPrimaryDescription
import com.nunchuk.android.main.membership.onchaintimelock.addkey.highlightedKeyIndices
import com.nunchuk.android.main.membership.onchaintimelock.addkey.isTimelockHighlighted
import com.nunchuk.android.model.TimelockBased
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.DraftInheritanceCardKey
import com.nunchuk.android.model.inheritance.FallbackInheritanceCondition
import com.nunchuk.android.model.inheritance.InheritanceSection
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType
import com.nunchuk.android.model.inheritance.PrimaryInheritanceCondition
import com.nunchuk.android.type.SignerType

/**
 * REP-01 below the "Before the timelock" heading: spending keys and the Platform key, then primary
 * inheritance (its keys and timelock) and each fallback, all in the server's card order. Only the
 * primary timelock has Change here; changing a fallback's time comes with the fallback phase.
 */
internal fun LazyListScope.configurableReplaceItems(
    state: ConfigurableReplaceState,
    onReplaceClicked: (ReplaceKeyRow) -> Unit,
    onVerifyClicked: (ReplaceKeyRow) -> Unit,
    onRemoveClicked: (originalXfp: String) -> Unit,
    onChangePrimaryTimelock: (WalletTimelock?) -> Unit,
) {
    val rowItem: @Composable (ReplaceKeyRow, Set<Int>) -> Unit = { row, highlighted ->
        ConfigurableReplaceKeyRow(
            row = row,
            target = state.targetOf(row),
            isHighlighted = row.keyIndices.any { it in highlighted },
            onReplaceClicked = { onReplaceClicked(row) },
            onVerifyClicked = { onVerifyClicked(row) },
            onRemoveClicked = { onRemoveClicked(row.originalXfp) },
        )
    }
    val spendingCards = state.cards.filter { it.section == InheritanceSection.SPENDING }
    items(spendingCards.filterNot { it.isServerKey }.flatMap { it.rows }) { row -> rowItem(row, emptySet()) }
    if (spendingCards.any { it.isServerKey }) {
        item { ReplaceServerCard() }
    }
    item {
        Text(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.nc_membership_pull_to_refresh_key_status),
            style = NunchukTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    val primary = state.policy?.primary
    val primaryHighlighted = state.validationErrors.highlightedKeyIndices(InheritanceSection.PRIMARY, null)
    item {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = afterTimelockPrimaryDescription(primary),
                style = NunchukTheme.typography.body,
            )
            PrimaryInheritanceHeader(primary)
        }
    }
    items(state.cards.filter { it.section == InheritanceSection.PRIMARY }.flatMap { it.rows }) { row ->
        rowItem(row, primaryHighlighted)
    }
    item {
        Column {
            ReplaceTimelockCard(
                originalTimelock = state.primaryTimelock.original,
                newTimelock = state.primaryTimelock.pending,
                isHighlighted = state.validationErrors.isTimelockHighlighted(InheritanceSection.PRIMARY, null),
                onChangeClicked = { onChangePrimaryTimelock(state.primaryTimelock.effective) },
            )
            InheritanceValidationErrors(state.validationErrors.filter { it.section == InheritanceSection.PRIMARY })
        }
    }

    val fallbacks = state.policy?.fallbacks.orEmpty()
    if (fallbacks.isNotEmpty()) {
        item {
            Column {
                HorizontalDivider(color = colorResource(id = R.color.nc_stroke_primary))
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_replace_fallbacks_section),
                    style = NunchukTheme.typography.title,
                )
            }
        }
    }
    fallbacks.forEach { fallback ->
        val highlighted = state.validationErrors.highlightedKeyIndices(InheritanceSection.FALLBACK, fallback.index)
        item { FallbackReplaceHeader(fallback) }
        items(
            state.cards
                .filter { it.section == InheritanceSection.FALLBACK && it.card.fallbackIndex == fallback.index }
                .flatMap { it.rows }
        ) { row -> rowItem(row, highlighted) }
        item {
            val timelock = state.fallbackTimelocks[fallback.index]
            Column {
                ReplaceTimelockCard(
                    originalTimelock = timelock?.original,
                    newTimelock = timelock?.pending,
                    isHighlighted = state.validationErrors.isTimelockHighlighted(InheritanceSection.FALLBACK, fallback.index),
                    onChangeClicked = null,
                )
                InheritanceValidationErrors(
                    state.validationErrors.filter {
                        it.section == InheritanceSection.FALLBACK && it.fallbackIndex == fallback.index
                    }
                )
            }
        }
    }

    // Issues that name no condition (e.g. the whole candidate): shown once, at the end.
    val otherErrors = state.validationErrors.filter {
        it.section != InheritanceSection.PRIMARY && it.section != InheritanceSection.FALLBACK
    }
    if (otherErrors.isNotEmpty()) {
        item { InheritanceValidationErrors(otherErrors) }
    }
}

/** "Fallback N" and "Requires M of N keys" (REP-01). */
@Composable
private fun FallbackReplaceHeader(fallback: FallbackInheritanceCondition) {
    Column {
        Text(
            text = stringResource(R.string.nc_replace_fallback_title, fallback.index + 1),
            style = NunchukTheme.typography.title,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = pluralStringResource(
                if (fallback.m == fallback.n) R.plurals.nc_replace_fallback_requires_all else R.plurals.nc_replace_fallback_requires_any,
                fallback.n,
                fallback.m,
                fallback.n,
            ),
            style = NunchukTheme.typography.bodySmall,
        )
    }
}

/**
 * One key of a configurable wallet's card. Its look follows the replacement of the whole key, which
 * may span cards (REP-02 / REP-03 / REP-03b): not replaced → Replace; some accounts in (missing
 * ones greyed, dashed) → Replace to continue; all in → Verify; verified → Remove.
 */
@Composable
private fun ConfigurableReplaceKeyRow(
    row: ReplaceKeyRow,
    target: ReplaceTarget?,
    isHighlighted: Boolean,
    onReplaceClicked: () -> Unit,
    onVerifyClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
) {
    val isReplacing = target?.isReplacing == true
    val device: SignerModel? = if (isReplacing) target?.replacementDevice else row.original
    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    val background = when {
        !isReplacing -> colorResource(id = R.color.nc_background_primary)
        target?.isComplete != true -> colorResource(id = R.color.nc_fill_slime).copy(alpha = 0.4f)
        target.isVerified -> colorResource(id = R.color.nc_fill_slime)
        else -> colorResource(id = R.color.nc_fill_beewax)
    }
    val borderColor = if (isHighlighted) MaterialTheme.colorScheme.error else colorResource(id = R.color.nc_stroke_primary)
    Column(modifier = Modifier.fillMaxWidth()) {
        NcDashLineBox(
            modifier = if (!isReplacing || isHighlighted) {
                Modifier.border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            } else {
                Modifier
            },
            showDashedBorder = isReplacing && target?.isComplete != true,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = background, shape = RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NcCircleImage(resId = device?.toReadableDrawableResId() ?: R.drawable.ic_hardware_key)
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = device?.name ?: stringResource(R.string.nc_membership_unknown_signer),
                        style = NunchukTheme.typography.body,
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NcTag(
                            label = device?.toReadableSignerType(context = LocalContext.current)
                                ?: stringResource(R.string.nc_unknown),
                            backgroundColor = colorResource(id = R.color.nc_bg_mid_gray),
                        )
                        row.slots.forEach { slot ->
                            NcTag(
                                modifier = Modifier.alpha(if (isReplacing && slot.replacement == null) 0.5f else 1f),
                                label = stringResource(R.string.nc_acct_x, slot.accountIndex),
                                backgroundColor = colorResource(id = R.color.nc_bg_mid_gray),
                            )
                        }
                    }
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = device?.getXfpOrCardIdLabel().orEmpty(),
                        style = NunchukTheme.typography.bodySmall,
                    )
                }
                val (label, onClick) = when {
                    !isReplacing || target?.isComplete != true -> stringResource(R.string.nc_replace) to onReplaceClicked
                    !target.isVerified -> stringResource(R.string.nc_verify) to onVerifyClicked
                    else -> stringResource(R.string.nc_remove) to onRemoveClicked
                }
                NcOutlineButton(
                    modifier = Modifier.height(36.dp),
                    contentPadding = contentPadding,
                    onClick = onClick,
                ) {
                    Text(text = label, style = NunchukTheme.typography.captionTitle)
                }
            }
        }
        val original = row.original
        if (isReplacing && original != null) {
            Row(
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_replace),
                    contentDescription = stringResource(R.string.nc_membership_cd_replace_icon),
                )
                Text(
                    text = stringResource(
                        R.string.nc_membership_replacing_signer,
                        original.name,
                        original.getXfpOrCardIdLabel(),
                    ),
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ConfigurableReplaceItemsPreview() {
    val keystone = previewSigner("Keystone", "79eb35f4", SignerType.AIRGAP)
    val coldcard = previewSigner("COLDCARD", "a47c3e19", SignerType.COLDCARD_NFC)
    val jade = previewSigner("Jade 2", "6b35f210", SignerType.AIRGAP)
    val passport = previewSigner("Passport", "8e41b2c7", SignerType.AIRGAP)
    val timelock = WalletTimelock(timelockValue = 2063999100L, timezone = "Asia/Jakarta", based = TimelockBased.TIME_LOCK)
    val state = ConfigurableReplaceState(
        policy = OnChainInheritancePolicy(
            type = OnChainInheritancePolicyType.CONFIGURABLE,
            primary = PrimaryInheritanceCondition(m = 2, n = 4, isCustomized = false, timelock = timelock),
            fallbacks = emptyList(),
        ),
        cards = listOf(
            previewCard(
                InheritanceSection.SPENDING,
                ReplaceSlot(0, 0, InheritanceSection.SPENDING, keystone, passport, VerifyType.NONE),
                ReplaceSlot(1, 1, InheritanceSection.PRIMARY, keystone, passport, VerifyType.NONE),
            ),
            previewCard(
                InheritanceSection.SPENDING,
                ReplaceSlot(2, 0, InheritanceSection.SPENDING, coldcard),
                ReplaceSlot(3, 1, InheritanceSection.PRIMARY, coldcard),
            ),
            ReplaceCard(
                card = DraftInheritanceCard(InheritanceSection.SPENDING, null, true, emptyList(), emptyList(), emptyMap()),
                rows = emptyList(),
            ),
            previewCard(InheritanceSection.PRIMARY, ReplaceSlot(6, 0, InheritanceSection.PRIMARY, jade)),
        ),
        primaryTimelock = ReplaceTimelock(original = timelock, pending = timelock.copy(timelockValue = 2095535100L)),
        fallbackTimelocks = emptyMap(),
        validationErrors = emptyList(),
    )
    NunchukTheme {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            configurableReplaceItems(
                state = state,
                onReplaceClicked = {},
                onVerifyClicked = {},
                onRemoveClicked = {},
                onChangePrimaryTimelock = {},
            )
        }
    }
}

private fun previewSigner(name: String, xfp: String, type: SignerType) = SignerModel(
    id = xfp,
    name = name,
    derivationPath = "",
    fingerPrint = xfp,
    type = type,
    isMasterSigner = false,
)

private fun previewCard(section: InheritanceSection, vararg slots: ReplaceSlot) = ReplaceCard(
    card = DraftInheritanceCard(
        section = section,
        fallbackIndex = null,
        isServerKey = false,
        tags = emptyList(),
        keys = slots.map { DraftInheritanceCardKey(it.keyIndex, it.accountIndex, it.section) },
        accountIndexByXfp = emptyMap(),
    ),
    rows = listOf(ReplaceKeyRow(slots.toList())),
)
