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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.main.R

@Composable
fun AddInheritanceKeyScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onAddKeyClick: () -> Unit = {},
    isFirstKey: Boolean = true,
    totalKeys: Int = 2,
    isShowKeyAdded: Boolean = false,
) {
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val backgroundRes = if (isFirstKey) {
        R.drawable.bg_add_inheritance_to_claim_first
    } else {
        R.drawable.bg_add_inheritance_to_claim_second
    }

    LaunchedEffect(isShowKeyAdded) {
        if (isShowKeyAdded) {
            snackbarHost.showNunchukSnackbar(
                message = context.getString(R.string.nc_error_add_same_key),
                type = NcToastType.ERROR
            )
        }
    }

    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        snackState = snackbarHost,
        topBar = {
            NcImageAppBar(
                backgroundRes = backgroundRes,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                val buttonText = if (isFirstKey) {
                    stringResource(R.string.nc_add_the_first_inheritance_key)
                } else {
                    stringResource(R.string.nc_add_the_second_inheritance_key)
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
                text = stringResource(R.string.nc_your_plan_requires, totalKeys),
                style = NunchukTheme.typography.heading,
                modifier = Modifier.padding(top = 24.dp)
            )

            if (isFirstKey) {
                Text(
                    text = stringResource(R.string.nc_add_first_key_desc),
                    style = NunchukTheme.typography.body
                )
            } else {
                Text(
                    text = stringResource(R.string.nc_successfully_added_the_first),
                    style = NunchukTheme.typography.body
                )
                Text(
                    text = stringResource(R.string.nc_add_second_key_desc),
                    style = NunchukTheme.typography.body
                )
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
        AddInheritanceKeyScreen(isFirstKey = false)
    }
}
