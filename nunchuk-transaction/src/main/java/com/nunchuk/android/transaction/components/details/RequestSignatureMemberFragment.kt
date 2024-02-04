package com.nunchuk.android.transaction.components.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.util.fromMxcUriToMatrixDownloadUrl
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toTitle
import com.nunchuk.android.transaction.R
import com.nunchuk.android.utils.parcelableArrayList
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RequestSignatureMemberFragment : BaseComposeBottomSheet() {

    private val viewModel: RequestSignatureMemberViewModel by viewModels()

    private val args: RequestSignatureMemberFragmentArgs by lazy { RequestSignatureMemberFragmentArgs.deserializeFrom(arguments) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(args.members)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NunchukTheme {
                    RequestSignatureMemberScreen(viewModel) {
                        setFragmentResult(
                            REQUEST_KEY,
                            Bundle().apply {
                                putString(
                                    EXTRA_MEMBER_ID,
                                    it
                                )
                            })
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "RequestSignatureMemberFragment"
        const val EXTRA_MEMBER_ID = "EXTRA_MEMBER_ID"


        private const val TAG = "RequestSignatureMemberFragment"

        private fun newInstance(members: List<ByzantineMember>) = RequestSignatureMemberFragment().apply {
            arguments = RequestSignatureMemberFragmentArgs(members).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, members: List<ByzantineMember>): RequestSignatureMemberFragment {
            return newInstance(members).apply { show(fragmentManager, TAG) }
        }

    }
}

data class RequestSignatureMemberFragmentArgs(val members: List<ByzantineMember>) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putParcelableArrayList(EXTRA_MEMBERS, members.toCollection(ArrayList()))
    }

    companion object {
        private const val EXTRA_MEMBERS = "EXTRA_MEMBERS"

        fun deserializeFrom(data: Bundle?) = RequestSignatureMemberFragmentArgs(
            data?.parcelableArrayList<ByzantineMember>(EXTRA_MEMBERS).orEmpty()
        )
    }
}


@Composable
fun RequestSignatureMemberScreen(
    viewModel: RequestSignatureMemberViewModel = viewModel(),
    onSelectMember: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RequestSignatureMemberScreenContent(
        members = state.members,
        onSelectMember = onSelectMember,
    )
}

@Composable
fun RequestSignatureMemberScreenContent(
    members: List<ByzantineMember> = emptyList(),
    onSelectMember: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(top = 16.dp),
            text = stringResource(id = R.string.nc_request_signature_dialog_title),
            style = NunchukTheme.typography.title
        )
        LazyColumn(
            modifier = Modifier
                .padding(top = 16.dp),
        ) {
            items(members) { member ->
                ContactMemberView(
                    email = member.user?.email ?: member.emailOrUsername,
                    name = member.user?.name.orEmpty(),
                    role = member.role,
                    avatarUrl = member.user?.avatar.orEmpty(),
                    isPendingMember = member.isContact().not(),
                    onSelectMember = {
                        onSelectMember(member.membershipId)
                    }
                )
            }
        }
    }
}

@Composable
private fun ContactMemberView(
    email: String = "",
    name: String = "",
    role: String = AssistedWalletRole.NONE.name,
    avatarUrl: String = "",
    isPendingMember: Boolean = false,
    onSelectMember: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .padding(vertical = 8.dp)
            .clickable {
                onSelectMember()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPendingMember.not()) {
                Box(
                    modifier = Modifier
                        .size(48.dp, 48.dp)
                        .clip(CircleShape)
                        .background(color = colorResource(id = R.color.nc_beeswax_light)),
                    contentAlignment = Alignment.Center
                ) {
                    GlideImage(
                        imageModel = { avatarUrl.fromMxcUriToMatrixDownloadUrl() },
                        imageOptions = ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        ),
                        loading = {
                            Text(
                                text = name.shorten(),
                                style = NunchukTheme.typography.title
                            )
                        },
                        failure = {
                            Text(
                                text = name.shorten(),
                                style = NunchukTheme.typography.title
                            )
                        }
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = name,
                        style = NunchukTheme.typography.body
                    )
                    NcTag(
                        modifier = Modifier.padding(top = 4.dp),
                        label = role.toTitle()
                    )
                    Text(
                        modifier = Modifier,
                        text = email,
                        style = NunchukTheme.typography.bodySmall
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .weight(1f, false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp, 48.dp)
                                .clip(CircleShape)
                                .background(color = colorResource(id = R.color.nc_whisper_color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_account_member),
                                contentDescription = ""
                            )
                        }

                        Column(
                            modifier = Modifier
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = email,
                                style = NunchukTheme.typography.body,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            NcTag(
                                modifier = Modifier.padding(top = 4.dp),
                                label = role.toTitle()
                            )
                        }
                    }
                    Text(
                        text = "Pending",
                        style = NunchukTheme.typography.title
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RequestSignatureMemberScreenContentPreview() {
    val members = arrayListOf<ByzantineMember>()
    NunchukTheme {
        RequestSignatureMemberScreenContent(members = members)
    }
}