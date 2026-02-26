---
name: android-activity-compose-navigation
description: Creates Android Activity with Compose screens, type-safe navigation, shared activity ViewModel, and NcScaffold with snackbar. Use when creating new activities, multi-screen flows, or when the user asks about activity + Compose + navigation patterns.
---

# Android Activity with Compose Navigation

Pattern for activities that host multiple Compose screens with shared state, type-safe navigation, and centralized snackbar handling.

## Activity Structure

```kotlin
@AndroidEntryPoint
class MyActivity : BaseNfcActivity<ViewBinding>() {  // or ComponentActivity
    private val viewModel: MyViewModel by viewModels()

    override fun initializeBinding(): ViewBinding = ViewBinding {
        ComposeView(this).apply {
            setContent {
                MyGraph(
                    activity = this@MyActivity,
                    activityViewModel = viewModel,
                    // other deps: navigator, pushEventManager, etc.
                )
            }
        }
    }.also { enableEdgeToEdge() }
}
```

## Graph Composable

- `rememberNavController()` and `remember { SnackbarHostState() }` at graph root
- Single `SnackbarHostState` passed to all screen destinations
- `LaunchedEffect(sharedUiState.event)` to handle ViewModel events (navigation, snackbar, dialogs)
- `NunchukTheme` wraps the `NavHost`
- `NcLoadingDialog()` when `sharedUiState.isLoading`

```kotlin
@Composable
private fun MyGraph(
    activity: Activity,
    activityViewModel: MyViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val sharedUiState by activityViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sharedUiState.event) {
        sharedUiState.event?.let { event ->
            when (event) {
                is MyEvent.ShowError -> {
                    snackbarHostState.showNunchukSnackbar(
                        message = event.message,
                        type = NcToastType.ERROR
                    )
                }
                is MyEvent.NavigateToX -> navController.navigateToX()
                // ...
            }
            activityViewModel.onEventHandled()
        }
    }

    NunchukTheme {
        if (sharedUiState.isLoading) NcLoadingDialog()
        NavHost(navController = navController, startDestination = StartRoute) {
            screenA(snackState = snackbarHostState, onBackPressed = { navController.popBackStack() })
            screenB(snackState = snackbarHostState, ...)
        }
    }
}
```

## Shared Activity ViewModel in Destinations

Screens inside `NavHost` need the activity-scoped ViewModel. Use:

```kotlin
composable<MyRoute> {
    val activity = LocalActivity.current as ComponentActivity
    val activityViewModel: MyViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val sharedData by activityViewModel.data.collectAsStateWithLifecycle()

    MyScreen(
        snackState = snackState,
        sharedData = sharedData,
        onBackPressed = onBackPressed,
        onSuccess = onSuccess,
    )
}
```

## Navigation Extension Pattern

Each destination has a navigation file:

```kotlin
// MyScreenNavigation.kt
@Serializable
data object MyScreenRoute  // or data class MyScreenRoute(val id: String) for args

fun NavGraphBuilder.myScreen(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onSuccess: (Result) -> Unit = {},
) {
    composable<MyScreenRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: MyViewModel = hiltViewModel(viewModelStoreOwner = activity)
        val data by activityViewModel.data.collectAsStateWithLifecycle()

        MyScreen(
            snackState = snackState,
            data = data,
            onBackPressed = onBackPressed,
            onSuccess = onSuccess,
        )
    }
}

fun NavController.navigateToMyScreen() {
    navigate(MyScreenRoute)
}
```

## NcScaffold and Snackbar

- Use `NcScaffold` with `snackState` for screens that need snackbar
- `contentWindowInsets` for edge-to-edge: `ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars)`

```kotlin
NcScaffold(
    modifier = modifier.navigationBarsPadding().imePadding(),
    contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars),
    snackState = snackState,
    bottomBar = { /* optional */ },
) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) { ... }
}
```

Show snackbar from screen (e.g. in LaunchedEffect when handling local ViewModel events):

```kotlin
snackState.showNunchukSnackbar(
    message = errorMessage,
    type = NcToastType.ERROR
)
```

## ViewModel Event Pattern

- `ClaimUiState(event: MyEvent? = null, isLoading: Boolean = false, ...)`
- Emit events from ViewModel; graph handles them and calls `onEventHandled()` to clear
- Events drive: navigation, snackbar, activity results (launchers)

## Activity Result Launchers

For flows that return data (e.g. picker, signer intro):

```kotlin
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    if (it.resultCode == Activity.RESULT_OK) {
        val data = it.data?.parcelable<Model>(GlobalResultKey.EXTRA)
        data?.let { activityViewModel.handleResult(it) }
        navController.popBackStack<PreviousRoute>(false)
    }
}
// Trigger: navigator.openSomeScreen(launcher = launcher, activityContext = activity)
```

## Key Imports

- `com.nunchuk.android.compose.NcScaffold`
- `com.nunchuk.android.compose.NcToastType`
- `com.nunchuk.android.compose.showNunchukSnackbar`
- `com.nunchuk.android.compose.dialog.NcLoadingDialog`
- `androidx.activity.compose.LocalActivity`
- `androidx.hilt.navigation.compose.hiltViewModel`
- `androidx.lifecycle.compose.collectAsStateWithLifecycle`
- `kotlinx.serialization.Serializable` for routes
