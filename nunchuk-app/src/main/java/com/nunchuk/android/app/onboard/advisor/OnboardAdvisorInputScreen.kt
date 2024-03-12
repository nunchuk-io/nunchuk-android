@file:OptIn(ExperimentalMaterial3Api::class)

package com.nunchuk.android.app.onboard.advisor

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.R
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.model.Country
import com.nunchuk.android.utils.EmailValidator

@Composable
fun OnboardAdvisorInputScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardAdvisorInputViewModel = hiltViewModel(),
    onSkip: () -> Unit = {},
    onOpenMainScreen: () -> Unit = {},
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(state.sendQuerySuccess) {
        if (state.sendQuerySuccess != null) {
            onOpenMainScreen()
            viewModel.onSendQuerySuccessEventConsumed()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackState.showSnackbar(
                NcSnackbarVisuals(
                    type = NcToastType.ERROR,
                    message = it,
                )
            )
            viewModel.onErrorMessageEventConsumed()
        }
    }

    OnboardAdvisorInputContent(
        modifier = modifier,
        uiState = state,
        onSkip = onSkip,
        onCountrySelected = { viewModel.onCountrySelected(it) },
        onEmailChanged = { viewModel.onEmailChanged(it) },
        onNoteChanged = { viewModel.onNoteChanged(it) },
        onSendQuery = { viewModel.onSendQuery() }
    )
}

@Composable
fun OnboardAdvisorInputContent(
    modifier: Modifier = Modifier,
    uiState: OnboardAdvisorInputUiState,
    onSkip: () -> Unit = {},
    onCountrySelected: (Country) -> Unit = {},
    onEmailChanged: (String) -> Unit = {},
    onNoteChanged: (String) -> Unit = {},
    onSendQuery: () -> Unit = {},
) {

    val context = LocalContext.current

    var showSelectCountrySheet by remember {
        mutableStateOf(false)
    }

    NunchukTheme {
        Scaffold(
            modifier = modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "",
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Text(
                            modifier = Modifier
                                .padding(16.dp)
                                .clickable(onClick = onSkip),
                            text = stringResource(id = R.string.nc_text_skip),
                            style = NunchukTheme.typography.textLink
                        )
                    },
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSendQuery,
                        content = { Text(text = stringResource(id = R.string.nc_send_query)) },
                        enabled = uiState.selectedCountry != null && uiState.email.isNotBlank() && EmailValidator.valid(
                            uiState.email
                        )
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .clickable {
                                context.openExternalLink("https://nunchuk.io/individuals")
                            },
                        text = stringResource(R.string.nc_learn_more_about_assisted_services),
                        style = NunchukTheme.typography.title,
                        textAlign = TextAlign.Center,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.nc_dont_have_an_advisor),
                    style = NunchukTheme.typography.heading
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.nc_dont_have_an_advisor_desc),
                    style = NunchukTheme.typography.body
                )

                Spacer(modifier = Modifier.height(16.dp))

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    title = stringResource(id = R.string.nc_your_country),
                    value = uiState.selectedCountry?.name
                        ?: stringResource(id = R.string.nc_select_a_country),
                    enabled = false,
                    onClick = {
                        showSelectCountrySheet = true
                    },
                    rightContent = {
                        Image(
                            modifier = Modifier
                                .padding(end = 12.dp),
                            painter = painterResource(id = com.nunchuk.android.main.R.drawable.ic_arrow),
                            contentDescription = ""
                        )
                    },
                    onValueChange = {

                    }
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    title = stringResource(id = R.string.nc_your_email_address),
                    value = uiState.email,
                    onValueChange = onEmailChanged
                )

                NcTextField(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .height(128.dp),
                    title = stringResource(id = R.string.nc_note),
                    titleHint = stringResource(id = R.string.nc_optional),
                    value = uiState.note,
                    inputBoxHeight = 128.dp,
                    onValueChange = onNoteChanged
                )

                Spacer(modifier = Modifier.height(16.dp))

            }

            if (showSelectCountrySheet) {
                NcSelectableBottomSheet(
                    options = uiState.countries.map { it.name },
                    selectedPos = uiState.countries.indexOf(uiState.selectedCountry),
                    onSelected = {
                        onCountrySelected(uiState.countries[it])
                        showSelectCountrySheet = false
                    },
                    onDismiss = { showSelectCountrySheet = false },
                    showSelectIndicator = true
                )
            }
        }
    }
}

@Preview
@Composable
fun OnboardAdvisorInputScreenPreview() {
    NunchukTheme {
        OnboardAdvisorInputScreen()
    }
}
