package com.nunchuk.android.main.membership.onchaintimelock.addkey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSecondaryButton
import com.nunchuk.android.compose.NcModalBottomSheet
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.onchaintimelock.setuptimelock.formatTimelockDate
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.inheritance.FallbackInheritanceCondition
import com.nunchuk.android.model.toTimelockExtra

/** A new fallback starts as 2 of 2 (FBK-01a). */
internal const val DEFAULT_FALLBACK_REQUIRED_KEYS = 2
internal const val DEFAULT_FALLBACK_TOTAL_KEYS = 2

/** The "Fallbacks" heading under primary inheritance (SET-09 / FBK-02). */
@Composable
internal fun FallbacksSectionHeader() {
    Column {
        HorizontalDivider(color = colorResource(id = R.color.nc_stroke_primary))
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(R.string.nc_replace_fallbacks_section),
            style = NunchukTheme.typography.title,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(R.string.nc_fallbacks_desc),
            style = NunchukTheme.typography.body,
        )
    }
}

/** "Fallback N" with Edit / Remove, and "Requires M of N keys" (FBK-02 / SET-11). */
@Composable
internal fun FallbackSetupHeader(
    fallback: FallbackInheritanceCondition,
    showActions: Boolean = true,
    enabled: Boolean = true,
    onEditClicked: () -> Unit = {},
    onRemoveClicked: () -> Unit = {},
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.nc_replace_fallback_title, fallback.index + 1),
                style = NunchukTheme.typography.title,
            )
            if (showActions) {
                UnderlinedAction(text = stringResource(R.string.nc_edit), enabled = enabled, onClick = onEditClicked)
                UnderlinedAction(
                    modifier = Modifier.padding(start = 24.dp),
                    text = stringResource(R.string.nc_remove),
                    enabled = enabled,
                    onClick = onRemoveClicked,
                )
            }
        }
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

@Composable
private fun UnderlinedAction(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        text = text,
        style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline),
    )
}

/** "+ Add fallback" (SET-09), "+ Add another fallback" once one exists (FBK-02 / SET-11). */
@Composable
internal fun AddFallbackButton(
    hasFallbacks: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    NcOutlineButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_plus_dark), contentDescription = null)
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(if (hasFallbacks) R.string.nc_add_another_fallback else R.string.nc_add_fallback),
        )
    }
}

/**
 * FBK-10 / FBK-11 "Remove Fallback N?". Later fallbacks move up one number and keep their keys and
 * unlock times; [fallbackCount] is how many fallbacks the setup has now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemoveFallbackSheet(
    fallback: FallbackInheritanceCondition,
    fallbackCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    NcModalBottomSheet(onDismissRequest = onDismiss) {
        RemoveFallbackContent(
            fallback = fallback,
            fallbackCount = fallbackCount,
            onConfirm = onConfirm,
            onCancel = onDismiss,
        )
    }
}

@Composable
private fun RemoveFallbackContent(
    fallback: FallbackInheritanceCondition,
    fallbackCount: Int,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val number = fallback.index + 1
    val description = buildString {
        append(pluralStringResource(R.plurals.nc_remove_fallback_desc, fallback.n))
        // FBK-10: any fallback after this one is renumbered; FBK-11 (the last one) says nothing more.
        if (number < fallbackCount) append(" ").append(stringResource(R.string.nc_remove_fallback_renumber))
    }
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.nc_remove_fallback_title, number),
            style = NunchukTheme.typography.title,
            textAlign = TextAlign.Center,
        )
        Text(text = description, style = NunchukTheme.typography.body)
        NcPrimaryDarkButton(modifier = Modifier.fillMaxWidth(), onClick = onConfirm) {
            Text(text = stringResource(R.string.nc_remove_fallback))
        }
        NcSecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = onCancel) {
            Text(text = stringResource(R.string.nc_cancel))
        }
    }
}

/**
 * KEY-10 "<key> is used in a fallback": importing the key here takes it out of [fallbacks]
 * (Continue), or nothing is imported (Cancel / dismiss).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KeyUsedInFallbackSheet(
    keyName: String,
    fallbacks: List<FallbackInheritanceCondition>,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    NcModalBottomSheet(onDismissRequest = onDismiss) {
        KeyUsedInFallbackContent(
            keyName = keyName,
            fallbacks = fallbacks,
            onContinue = onContinue,
            onCancel = onDismiss,
        )
    }
}

@Composable
private fun KeyUsedInFallbackContent(
    keyName: String,
    fallbacks: List<FallbackInheritanceCondition>,
    onContinue: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.nc_key_used_in_fallback_title, keyName),
            style = NunchukTheme.typography.title,
            textAlign = TextAlign.Center,
        )
        Text(text = stringResource(R.string.nc_key_used_in_fallback_desc), style = NunchukTheme.typography.body)
        fallbacks.forEach { fallback ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.greyLight, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.nc_fallback_number, fallback.index + 1),
                    style = NunchukTheme.typography.title,
                )
                fallback.timelock?.takeIf { it.timelockValue > 0 }?.let { timelock ->
                    Text(
                        text = stringResource(
                            R.string.nc_miniscript_after_value,
                            formatTimelockDate(timelock.toTimelockExtra()),
                        ),
                        style = NunchukTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                }
            }
        }
        NcPrimaryDarkButton(modifier = Modifier.fillMaxWidth(), onClick = onContinue) {
            Text(text = stringResource(R.string.nc_text_continue))
        }
        NcSecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = onCancel) {
            Text(text = stringResource(R.string.nc_cancel))
        }
    }
}

private val previewFallback = FallbackInheritanceCondition(index = 0, m = 2, n = 3, timelock = null)

@PreviewLightDark
@Composable
private fun KeyUsedInFallbackContentPreview() {
    NunchukTheme {
        KeyUsedInFallbackContent(
            keyName = "Jade",
            fallbacks = listOf(
                previewFallback.copy(timelock = WalletTimelock(timelockValue = 2221988700L, timezone = "Asia/Bangkok")),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun FallbackSetupViewsPreview() {
    NunchukTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FallbacksSectionHeader()
            FallbackSetupHeader(fallback = previewFallback)
            FallbackSetupHeader(fallback = previewFallback.copy(index = 1, m = 1, n = 1))
            AddFallbackButton(hasFallbacks = true)
        }
    }
}

@PreviewLightDark
@Composable
private fun RemoveFallbackContentPreview() {
    NunchukTheme {
        RemoveFallbackContent(fallback = previewFallback, fallbackCount = 2)
    }
}
