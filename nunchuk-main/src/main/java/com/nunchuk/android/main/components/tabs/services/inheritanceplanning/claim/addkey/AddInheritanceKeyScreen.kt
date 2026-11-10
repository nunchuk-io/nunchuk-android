package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

/**
 * "Your plan requires N inheritance keys", before each key of a claim. [index] is the 1-based key
 * about to be added; past [totalKeys] the backend asked for another key without a complete
 * condition, so the copy only counts what was added.
 */
@Composable
fun AddInheritanceKeyScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    onAddKeyClick: () -> Unit = {},
    index: Int = 1,
    totalKeys: Int = 2,
) {
    val isFirstKey = index <= 1
    // The drawn copy for a 2-key plan; any other later key uses the "x of N" copy.
    val isSecondOfTwo = index == 2 && totalKeys == 2
    val addedKeys = index - 1
    val backgroundRes = if (isFirstKey) {
        R.drawable.bg_add_inheritance_to_claim_first
    } else {
        R.drawable.bg_add_inheritance_to_claim_second
    }

    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = backgroundRes,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                val buttonText = when {
                    isFirstKey -> stringResource(R.string.nc_add_the_first_inheritance_key)
                    isSecondOfTwo -> stringResource(R.string.nc_add_the_second_inheritance_key)
                    else -> stringResource(R.string.nc_claim_add_another_key)
                }

                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAddKeyClick
                ) {
                    Text(text = buttonText)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = pluralStringResource(R.plurals.nc_your_plan_requires, totalKeys, totalKeys),
                style = NunchukTheme.typography.heading,
                modifier = Modifier.padding(top = 24.dp)
            )

            when {
                isFirstKey -> Text(
                    text = stringResource(R.string.nc_add_first_key_desc, totalKeys),
                    style = NunchukTheme.typography.body
                )

                isSecondOfTwo -> {
                    Text(
                        text = stringResource(R.string.nc_successfully_added_the_first),
                        style = NunchukTheme.typography.body
                    )
                    Text(
                        text = stringResource(R.string.nc_add_second_key_desc),
                        style = NunchukTheme.typography.body
                    )
                }

                else -> {
                    Text(
                        text = if (addedKeys < totalKeys) {
                            stringResource(R.string.nc_claim_keys_added_progress, addedKeys, totalKeys)
                        } else {
                            stringResource(R.string.nc_claim_keys_added, addedKeys)
                        },
                        style = NunchukTheme.typography.body
                    )
                    Text(
                        text = stringResource(R.string.nc_claim_add_another_key_to_continue),
                        style = NunchukTheme.typography.body
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AddInheritanceKeyScreenPreview() {
    NunchukTheme {
        AddInheritanceKeyScreen()
    }
}

@PreviewLightDark
@Composable
private fun AddInheritanceKeyScreen2Preview() {
    NunchukTheme {
        AddInheritanceKeyScreen(index = 2)
    }
}

@PreviewLightDark
@Composable
private fun AddInheritanceKeyScreenNextOfThreePreview() {
    NunchukTheme {
        AddInheritanceKeyScreen(index = 3, totalKeys = 3)
    }
}
