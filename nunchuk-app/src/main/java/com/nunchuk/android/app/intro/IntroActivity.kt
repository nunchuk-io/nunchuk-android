package com.nunchuk.android.app.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.R
import com.nunchuk.android.app.splash.GuestModeEvent
import com.nunchuk.android.compose.NcPrimaryButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class IntroActivity : BaseComposeActivity() {
    private val viewModel: GuestModeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()

        setContent {
            NunchukTheme {
                IntroScreen(viewModel)
            }
        }

        subscribeEvents()
    }

    private fun handleInitGuestModeNunchukSuccess() {
        hideLoading()
        finish()
        navigator.openMainScreen(this)
        overridePendingTransition(0, 0)
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: GuestModeEvent) {
        when (event) {
            GuestModeEvent.InitSuccessEvent -> handleInitGuestModeNunchukSuccess()
            is GuestModeEvent.InitErrorEvent -> NCToastMessage(this).showError(event.error)
            is GuestModeEvent.LoadingEvent -> showLoading()
            GuestModeEvent.OpenSignInScreen -> {
                navigator.openSignInScreen(this, isNeedNewTask = false)
                finish()
            }
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, IntroActivity::class.java))
        }
    }
}

@Composable
internal fun IntroScreen(viewModel: GuestModeViewModel = viewModel()) {
    IntroContent(
        initGuestModeNunchuk = viewModel::initGuestModeNunchuk,
        openSignInScreen = viewModel::openSignInScreen
    )
}

@Composable
internal fun IntroContent(
    initGuestModeNunchuk: () -> Unit = {},
    openSignInScreen: () -> Unit = {}
) {
    Surface(
        color = colorResource(id = R.color.nc_primary_color),
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.0f))
            Image(painter = painterResource(id = R.drawable.ic_logo_light), contentDescription = "Logo")
            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = stringResource(id = R.string.nc_text_welcome_to_nunchuk),
                style = NunchukTheme.typography.heading.copy(
                    color = colorResource(
                        id = R.color.nc_white_color
                    )
                )
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(id = R.string.nc_text_intro),
                style = NunchukTheme.typography.body.copy(
                    color = colorResource(
                        id = R.color.nc_white_color
                    )
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(3.0f))
            NcPrimaryButton(onClick = initGuestModeNunchuk) {
                Text(
                    text = stringResource(id = R.string.nc_text_get_started),
                    style = NunchukTheme.typography.title
                )
            }
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                onClick = openSignInScreen,
            ) {
                Text(
                    text = stringResource(id = R.string.nc_text_sign_in),
                    style = NunchukTheme.typography.title.copy(color = MaterialTheme.colors.onPrimary)
                )
            }
        }
    }
}

@Preview
@Composable
fun IntroContentPreview() {
    NunchukTheme {
        IntroContent()
    }
}



