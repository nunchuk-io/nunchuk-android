package com.nunchuk.android.main.components.tabs.services

import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ServicesFragment : Fragment()
//
//    @Inject
//    lateinit var navigator: NunchukNavigator
//
//    private val viewModel: ServicesViewModel by viewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
//    ): View {
//        return ComposeView(requireContext()).apply {
//            setContent {
//                ServicesTabScreen(viewModel)
//            }
//        }
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        flowObserver(viewModel.event) {
//            when (it) {
//                is ServicesEvent.ItemClick -> {
//                    when (it.item) {
//                        ServiceTabRowItem.ClaimInheritance -> TODO()
//                        ServiceTabRowItem.CoSigningPolicies -> TODO()
//                        ServiceTabRowItem.EmergencyLockdown -> navigator.openEmergencyLockdownScreen(
//                            requireContext()
//                        )
//                        ServiceTabRowItem.KeyRecovery -> navigator.openKeyRecoveryScreen(
//                            requireContext()
//                        )
//                        ServiceTabRowItem.ManageSubscription -> TODO()
//                        ServiceTabRowItem.OrderNewHardware -> TODO()
//                        ServiceTabRowItem.RollOverAssistedWallet -> TODO()
//                        ServiceTabRowItem.SetUpInheritancePlan -> TODO()
//                    }
//                }
//                is ServicesEvent.Loading -> TODO()
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalLifecycleComposeApi::class)
//@Composable
//fun ServicesTabScreen(
//    viewModel: ServicesViewModel = viewModel()
//) {
//    val state by viewModel.state.collectAsStateWithLifecycle()
//
//    ServicesTabScreenContent(state, onItemClick = {
//        viewModel.onItemClick(it)
//    })
//}
//
//@Composable
//fun ServicesTabScreenContent(
//    state: ServicesState = ServicesState(), onItemClick: (ServiceTabRowItem) -> Unit = {}
//) {
//    NunchukTheme {
//        Scaffold { innerPadding ->
//            Column(
//                modifier = Modifier
//                    .padding(innerPadding)
//            ) {
//                Text(
//                    text = stringResource(R.string.nc_services_tab),
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                LazyColumn(
//                    modifier = Modifier.weight(1.0f),
//                    verticalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    state.rowItems.forEach {
//                        item {
//                            CategoryItem(title = it.title, drawableId = it.drawableId)
//                        }
//                        items(it.items) {
//                            ChildItem(title = it.title) {
//                                onItemClick(it)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun CategoryItem(title: Int, drawableId: Int) {
//    Row(
//        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Icon(painter = painterResource(id = drawableId), contentDescription = "")
//        Text(
//            text = stringResource(id = title),
//            modifier = Modifier.padding(start = 8.dp),
//            style = NunchukTheme.typography.title
//        )
//    }
//}
//
//@Composable
//private fun ChildItem(title: Int, onItemClick: () -> Unit = {}) {
//    Row(
//        modifier = Modifier
//            .padding(start = 16.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)
//            .clickable {
//                onItemClick()
//            },
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = stringResource(id = title),
//            modifier = Modifier.weight(weight = 1f),
//            style = NunchukTheme.typography.body
//        )
//        Icon(painter = painterResource(id = R.drawable.ic_arrow), contentDescription = "")
//    }
//}
//
//@Preview
//@Composable
//private fun CategoryItemPreview(
//    title: Int = R.string.nc_emergency, drawableId: Int = R.drawable.ic_emergency
//) {
//    CategoryItem(title, drawableId)
//}
//
//@Preview
//@Composable
//private fun ChildItemPreview(
//    title: Int = R.string.nc_emergency
//) {
//    ChildItem(title)
//}
//
//@Preview
//@Composable
//private fun ServicesTabScreenContentPreview() {
//    ServicesTabScreenContent()
//}