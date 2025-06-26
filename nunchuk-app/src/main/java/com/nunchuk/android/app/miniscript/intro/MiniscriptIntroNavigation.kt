package com.nunchuk.android.app.miniscript.intro

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.miniscript.MiniscriptUtil
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.core.miniscript.SelectMultisignTypeBottomSheet
import com.nunchuk.android.main.R
import com.nunchuk.android.type.AddressType
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.BufferedReader

@Serializable
object MiniscriptIntro

fun NavGraphBuilder.miniscriptIntroDestination(
    addressType: AddressType = AddressType.ANY,
    onSelect: (MultisignType) -> Unit = {},
    onNavigateToCustomTemplate: (String) -> Unit = {}
) {
    composable<MiniscriptIntro> {
        val viewModel = hiltViewModel<MiniscriptIntroViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current

        LaunchedEffect(uiState.event) {
            when (val event = uiState.event) {
                is MiniscriptIntroEvent.NavigateToCustomTemplate -> {
                    onNavigateToCustomTemplate(event.template)
                }

                is MiniscriptIntroEvent.ShowError -> {
                    Toast.makeText(
                        context,
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                null -> {}
            }
            viewModel.onEventHandled()
        }

        MiniscriptIntroScreen(
            addressType = addressType,
            viewModel = viewModel,
            onSelect = onSelect
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniscriptIntroScreen(
    addressType: AddressType = AddressType.ANY,
    viewModel: MiniscriptIntroViewModel,
    onSelect: (MultisignType) -> Unit = {},
) {
    var showSelectMultisignTypeBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        Timber.tag("miniscript-feature").d("File picked: $uri")
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = BufferedReader(inputStream.reader()).use { reader ->
                        reader.readText()
                    }
                    viewModel.handleFileContent(MiniscriptUtil.revertFormattedMiniscript(content), addressType)
                }
            } catch (e: Exception) {
                Timber.e("Error reading file: $e")
                Toast.makeText(
                    context,
                    "Error reading file: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.miniscript_illustration,
                    title = "",
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showSelectMultisignTypeBottomSheet = true
                        }) {
                        Text(text = "Continue")
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_miniscript),
                    style = NunchukTheme.typography.heading
                )
                NcHighlightText(
                    modifier = Modifier.padding(vertical = 16.dp),
                    text = stringResource(R.string.nc_miniscript_desc),
                    style = NunchukTheme.typography.body
                )
            }
        }

        if (showSelectMultisignTypeBottomSheet) {
            SelectMultisignTypeBottomSheet(
                onDismiss = { showSelectMultisignTypeBottomSheet = false },
                onSelect = { multisignType ->
                    if (multisignType == MultisignType.IMPORT) {
                        filePickerLauncher.launch("text/*")
                    } else {
                        onSelect(multisignType)
                    }
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
fun MiniscriptIntroScreenPreview() {
    MiniscriptIntroScreen(viewModel = hiltViewModel())
}