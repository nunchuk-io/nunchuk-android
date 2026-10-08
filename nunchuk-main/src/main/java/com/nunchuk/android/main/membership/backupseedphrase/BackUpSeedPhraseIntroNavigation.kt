package com.nunchuk.android.main.membership.backupseedphrase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import kotlinx.serialization.Serializable

@Serializable
object BackUpSeedPhraseIntro

fun NavGraphBuilder.backUpSeedPhraseIntroDestination(
    onContinue: () -> Unit = {},
    onMoreClicked: () -> Unit = {}
) {
    composable<BackUpSeedPhraseIntro> {
        BackUpSeedPhraseIntroScreen(
            onContinue = onContinue,
            onMoreClicked = onMoreClicked
        )
    }
}

@Composable
private fun BackUpSeedPhraseIntroScreen(
    viewModel: BackUpSeedPhraseSharedViewModel = hiltViewModel(),
    onContinue: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    BackUpSeedPhraseIntroContent(
        onContinueClicked = onContinue,
        remainTime = remainTime,
        onMoreClicked = onMoreClicked
    )
}

@Composable
private fun BackUpSeedPhraseIntroContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    // The seed-plate illustration the design asks for here is the numbered one —
                    // "Seed phrase" over words 1-12 — which the claim flow already carries.
                    backgroundRes = R.drawable.bg_inheritance_recover_seed_phrase,
                    title = if (remainTime <= 0) "" else stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = stringResource(R.string.nc_membership_cd_more_icon)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(R.string.nc_membership_i_have_backed_it_up))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_membership_backup_seed_phrase_title),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.nc_membership_backup_seed_phrase_steel_plate))
                        }
                        append(" ")
                        append(stringResource(R.string.nc_membership_backup_seed_phrase_share))
                    },
                    style = NunchukTheme.typography.body
                )

                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 1,
                    label = stringResource(R.string.nc_membership_backup_step_turn_on),
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 2,
                    label = stringResource(R.string.nc_membership_backup_step_find_recovery_words),
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 3,
                    label = stringResource(R.string.nc_membership_backup_step_write_words),
                )
            }
        }
    }
}

@Preview
@Composable
private fun BackUpSeedPhraseIntroScreenPreview() {
    BackUpSeedPhraseIntroContent(

    )
}