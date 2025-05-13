package com.nunchuk.android.app.miniscript.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.core.miniscript.MiniscriptUtil
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.type.AddressType
import kotlinx.serialization.Serializable

@Serializable
data class MiniscriptCustomTemplate(
    val template: String = "",
    val addressType: AddressType
)

fun NavGraphBuilder.miniscriptCustomTemplateDestination(onNext: (String) -> Unit = {}) {
    composable<MiniscriptCustomTemplate> { navBackStackEntry ->
        val data: MiniscriptCustomTemplate = navBackStackEntry.toRoute()
        val viewModel: MiniscriptCustomTemplateViewModel = hiltViewModel()
        val event by viewModel.event.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(event) {
            when (event) {
                is MiniscriptCustomTemplateEvent.Success -> {
                    onNext((event as MiniscriptCustomTemplateEvent.Success).template)
                    viewModel.clearEvent()
                }
                is MiniscriptCustomTemplateEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        NcSnackbarVisuals(
                            message = (event as MiniscriptCustomTemplateEvent.Error).message,
                            type = NcToastType.ERROR
                        )
                    )
                    viewModel.clearEvent()
                }
                null -> {}
            }
        }

        MiniscriptCustomTemplateScreen(
            template = MiniscriptUtil.formatMiniscriptCorrectly(data.template),
            onContinue = { template ->
                viewModel.createMiniscriptTemplate(MiniscriptUtil.revertFormattedMiniscript(template), data.addressType)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniscriptCustomTemplateScreen(
    template: String = "",
    onContinue: (String) -> Unit = {},
) {
    var miniscriptValue by remember { mutableStateOf(template) }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "Enter miniscript",
                    textStyle = NunchukTheme.typography.titleLarge,
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
                            if (miniscriptValue.isNotBlank()) {
                                onContinue(miniscriptValue)
                            }
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
                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    title = "Customize Miniscript",
                    placeholder = {
                        Text(
                            text = "Example: andor(\n" +
                                    "ln:older(12900),\n" +
                                    "thresh(2,pk(A), s:pk(B), s:pk(C)),\n" +
                                    "thresh(2,pk(A),s:pk(D),s:pk(E))\n" +
                                    ")",
                            style = NunchukTheme.typography.body.copy(
                                color = colorResource(
                                    id = R.color.nc_boulder_color
                                )
                            )
                        )
                    },
                    value = miniscriptValue,
                    minLines = 4,
                    onValueChange = {
                        miniscriptValue = it
                    }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewMiniscriptConfigTemplateScreen() {
    MiniscriptCustomTemplateScreen()
}