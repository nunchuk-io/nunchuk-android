package com.nunchuk.android.main.membership

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseIntroFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                BaseIntroScreen(
                    title = title,
                    styles = styles,
                    imageResId = imageResId,
                    content = content,
                    onContinueClicked = ::onContinueClicked
                )
            }
        }
    }

    abstract val title: String

    @get:DrawableRes
    abstract val imageResId: Int
    abstract val content: String
    open val styles: Map<SpanIndicator, SpanStyle> = emptyMap()
    abstract fun onContinueClicked()
}

@Composable
fun BaseIntroScreen(
    title: String = "",
    content: String = "",
    styles: Map<SpanIndicator, SpanStyle> = emptyMap(),
    @DrawableRes imageResId: Int = 0,
    onContinueClicked: () -> Unit = {},
) = NunchukTheme {
    Scaffold(modifier = Modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(
                backgroundRes = imageResId,
                backIconRes = R.drawable.ic_close
            )
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = title,
                style = NunchukTheme.typography.heading
            )
            NcSpannedText(
                modifier = Modifier.padding(16.dp),
                text = content,
                baseStyle = NunchukTheme.typography.body,
                styles = styles,
            )
        }
    }
}

@Preview
@Composable
fun IntroAssistedWalletScreenPreview() {
    BaseIntroScreen(
        title = "My Sceen",
        imageResId = R.drawable.nc_bg_intro_assisted_wallet,
        content = stringResource(R.string.nc_welcome_assisted_wallet_desc)
    )
}
