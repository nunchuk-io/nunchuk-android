package com.nunchuk.android.main.membership.onchaintimelock.addkey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRequiredKeysCounter
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.annotatedStringWithStyledArg
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.inheritance.PrimaryInheritanceCondition

/** The guided default primary: the 3 owner keys plus one separate inheritance key, any 2 of 4. */
internal const val DEFAULT_PRIMARY_REQUIRED_KEYS = 2
internal const val DEFAULT_PRIMARY_TOTAL_KEYS = 4

/** Upper bound for the policy sheet's counters; the backend validates the real policy. */
internal const val MAX_PRIMARY_TOTAL_KEYS = 20

/**
 * "After the timelock, …" above the primary inheritance card of a CONFIGURABLE draft: the default
 * 2-of-4 copy (SET-02), or the customized M of N (SET-08b).
 */
@Composable
internal fun afterTimelockPrimaryDescription(primary: PrimaryInheritanceCondition?): AnnotatedString {
    val bold = SpanStyle(fontWeight = FontWeight.W700)
    if (primary == null || !primary.isCustomized) {
        return annotatedStringWithStyledArg(
            formatRes = R.string.nc_membership_after_timelock_default_primary_desc,
            argRes = R.string.nc_membership_after_the_timelock,
            style = bold,
        )
    }
    // SET-08b / PRI-07: the whole sentence is bold, and the signature count is spelled out.
    val text = pluralStringResource(
        R.plurals.nc_membership_after_timelock_customized_primary_desc,
        primary.m,
        numberInWords(primary.m),
        primary.n,
    )
    return AnnotatedString(text, spanStyles = listOf(AnnotatedString.Range(bold, 0, text.length)))
}

/** "two" for 2; the digits past the words the resources spell out. */
@Composable
internal fun numberInWords(value: Int): String =
    stringArrayResource(R.array.nc_number_words).getOrNull(value) ?: value.toString()

/**
 * "Primary inheritance" with the Inheritance badge on the right and, once customized, "Requires any
 * M of N keys" (SET-02 / SET-08b).
 */
@Composable
internal fun PrimaryInheritanceHeader(primary: PrimaryInheritanceCondition?) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.nc_primary_inheritance_section),
                style = NunchukTheme.typography.title,
            )
            InheritanceBadge()
        }
        if (primary != null && primary.isCustomized) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = if (primary.m == primary.n) {
                    stringResource(R.string.nc_primary_inheritance_requires_all_keys, primary.m, primary.n)
                } else {
                    stringResource(R.string.nc_primary_inheritance_requires_any_keys, primary.m, primary.n)
                },
                style = NunchukTheme.typography.bodySmall,
            )
        }
    }
}

/** Backend `validation_errors`, each message shown once (PRI-07 / SET-13). */
@Composable
internal fun InheritanceValidationErrors(errors: List<InheritanceValidationError>) {
    if (errors.isEmpty()) return
    Column {
        errors.distinctBy { it.message }.forEach { error ->
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = error.message,
                style = NunchukTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** "Customize (advanced)" on the default primary, "Edit" once customized. */
@Composable
internal fun CustomizePrimaryInheritanceLink(
    isCustomized: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        text = stringResource(
            if (isCustomized) R.string.nc_edit else R.string.nc_customize_inheritance_advanced
        ),
        style = NunchukTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline),
    )
}

/** PRI-02 "Primary inheritance policy": M of N with the +/- buttons disabled at the limits. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrimaryInheritancePolicySheet(
    primary: PrimaryInheritanceCondition?,
    onContinue: (m: Int, n: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        dragHandle = {},
    ) {
        PrimaryInheritancePolicyContent(
            initialM = primary?.m ?: DEFAULT_PRIMARY_REQUIRED_KEYS,
            initialN = primary?.n ?: DEFAULT_PRIMARY_TOTAL_KEYS,
            onContinue = onContinue,
        )
    }
}

@Composable
private fun PrimaryInheritancePolicyContent(
    initialM: Int,
    initialN: Int,
    onContinue: (m: Int, n: Int) -> Unit = { _, _ -> },
) {
    var n by rememberSaveable { mutableIntStateOf(initialN.coerceIn(1, MAX_PRIMARY_TOTAL_KEYS)) }
    var m by rememberSaveable { mutableIntStateOf(initialM.coerceIn(1, n)) }
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.nc_primary_inheritance_policy),
            style = NunchukTheme.typography.title,
        )
        Spacer(modifier = Modifier.height(16.dp))
        NcRequiredKeysCounter(
            title = stringResource(R.string.nc_miniscript_required_keys),
            description = stringResource(R.string.nc_miniscript_num_signatures_required),
            value = m,
            enableIncrement = m < n,
            enableDecrement = m > 1,
            isValueBoxed = false,
            buttonSize = 48.dp,
            onIncrement = { if (m < n) m++ },
            onDecrement = { if (m > 1) m-- },
        )
        NcRequiredKeysCounter(
            title = stringResource(R.string.nc_miniscript_total_number_of_keys),
            description = stringResource(R.string.nc_miniscript_total_number_of_pubkeys),
            value = n,
            enableIncrement = n < MAX_PRIMARY_TOTAL_KEYS,
            enableDecrement = n > m,
            isValueBoxed = false,
            buttonSize = 48.dp,
            onIncrement = { if (n < MAX_PRIMARY_TOTAL_KEYS) n++ },
            onDecrement = { if (n > m) n-- },
        )
        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            onClick = { onContinue(m, n) },
        ) {
            Text(text = stringResource(R.string.nc_text_continue))
        }
    }
}

@PreviewLightDark
@Composable
private fun PrimaryInheritancePolicyContentPreview() {
    NunchukTheme {
        PrimaryInheritancePolicyContent(initialM = 2, initialN = 4)
    }
}

@PreviewLightDark
@Composable
private fun PrimaryInheritanceHeaderPreview() {
    NunchukTheme {
        Column(Modifier.padding(16.dp)) {
            PrimaryInheritanceHeader(
                primary = PrimaryInheritanceCondition(m = 3, n = 4, isCustomized = true, timelock = null)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomizePrimaryInheritanceLink(isCustomized = true, onClick = {})
            }
        }
    }
}
