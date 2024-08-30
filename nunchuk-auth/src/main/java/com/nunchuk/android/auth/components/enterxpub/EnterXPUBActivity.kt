package com.nunchuk.android.auth.components.enterxpub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.nunchuk.android.auth.components.authentication.SignInAuthenticationActivity
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EnterXPUBActivity : BaseComposeActivity() {

    private val viewModel: EnterXPUBViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                setContent {
                    EnterXPUBContent {
                        viewModel.signinDummy(it)
                    }
                }
            }
        )

        flowObserver(viewModel.event) { event ->
            when (event) {
                is EnterXPUBEvent.Error -> NCToastMessage(this).showError(event.message.orEmpty())
                is EnterXPUBEvent.Loading -> showOrHideLoading(event.loading)
                is EnterXPUBEvent.Success -> {
                    SignInAuthenticationActivity.start(
                        requiredSignatures = event.requiredSignatures,
                        launcher = null,
                        activityContext = this,
                        dummyTransactionId = event.dummyTransactionId,
                        signInData = event.signInData
                    )
                }
            }
        }
    }

    @Composable
    fun EnterXPUBContent(
        onContinueClick: (data: String) -> Unit = { }
    ) {
        val focusRequester = remember { FocusRequester() }
        var inputXPUB by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        NunchukTheme {
            Scaffold(
                modifier = Modifier
                    .navigationBarsPadding()
                    .statusBarsPadding(),
                topBar = {
                    NcTopAppBar(
                        title = "",
                        textStyle = NunchukTheme.typography.titleLarge
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NcPrimaryDarkButton(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth(),
                            enabled = inputXPUB.isNotEmpty(),
                            onClick = {
                                onContinueClick(inputXPUB)
                            }
                        ) {
                            Text(text = stringResource(R.string.nc_text_continue))
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
                        text = stringResource(id = R.string.nc_enter_xpub),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(id = R.string.nc_enter_xpub_desc),
                        style = NunchukTheme.typography.body
                    )
                    NcTextField(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .padding(top = 24.dp),
                        title = stringResource(id = R.string.nc_xpub),
                        value = inputXPUB,
                        inputBoxHeight = 130.dp,
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.nc_enter_xpub),
                                style = NunchukTheme.typography.body.copy(
                                    color = colorResource(
                                        id = R.color.nc_boulder_color
                                    )
                                )
                            )
                        },
                        onValueChange = {
                            inputXPUB = it
                        },
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EnterXPUBActivity::class.java))
        }
    }
}

@Preview
@Composable
fun EnterXPUBContentPreview() {
    EnterXPUBActivity().EnterXPUBContent()
}