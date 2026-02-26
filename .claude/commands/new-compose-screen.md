# New Compose Screen

Create a new Compose screen following the project's established architecture pattern using Compose Navigation, Hilt, ViewModel, and shared ViewModel communication.

## Instructions

The user will describe the new screen they want. Your job is to generate all required files following the patterns below. Ask clarifying questions if any of these are missing:

- **Screen name** (e.g., `ClaimMagicPhrase`) — derives all file names and class names
- **Feature package path** (e.g., `com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim`)
- **Route parameters** — none (use `data object`) or typed params (use `data class`)
- **Screen ViewModel needed?** — yes/no. If yes, does it need `@AssistedInject` (screen-specific params at construction time)?
- **Shared ViewModel** — which shared ViewModel this screen reads/writes (e.g., `ClaimInheritanceViewModel`)
- **Events emitted to shared ViewModel** — list of navigation/cross-screen events
- **Callbacks out of the nav function** — `onBackPressed`, `onContinue`, etc.

---

## Architecture Reference

This project uses the following layered pattern. Generate **all** files listed for each layer.

---

### Layer 1 — Route Object (`[ScreenName]Route.kt`)

**No parameters → `data object`:**
```kotlin
@Serializable
data object ClaimMagicPhraseRoute
```

**With parameters → `data class`:**
```kotlin
@Serializable
data class AddInheritanceKeyRoute(
    val index: Int,
    val totalKeys: Int,
)
```

Rules:
- Always annotate with `@Serializable` (kotlinx.serialization)
- Only primitive types or `@Serializable` types as parameters
- If a route needs a complex object, flatten it into primitives and add a conversion method to the data class

---

### Layer 2 — Navigate Extension (`[ScreenName]Navigation.kt`)

Contains two top-level functions in the same file:

**1. `NavController` extension — navigate to this screen:**
```kotlin
fun NavController.navigateToClaimMagicPhrase(navOptions: NavOptions? = null) {
    navigate(ClaimMagicPhraseRoute, navOptions)
}

// With parameters:
fun NavController.navigateToAddInheritanceKey(index: Int, totalKeys: Int) {
    navigate(
        AddInheritanceKeyRoute(index, totalKeys),
        navOptions {
            popUpTo<ClaimMagicPhraseRoute> { inclusive = false }
        }
    )
}
```

**2. `NavGraphBuilder` extension — register the destination:**
```kotlin
// Case A: no shared ViewModel access needed
fun NavGraphBuilder.claimMagicPhrase(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinue: (String, InheritanceClaimingInit) -> Unit,
) {
    composable<ClaimMagicPhraseRoute> {
        ClaimMagicPhraseScreen(
            snackState = snackState,
            onBackPressed = onBackPressed,
            onContinue = onContinue,
        )
    }
}

// Case B: shared ViewModel read access
fun NavGraphBuilder.claimNote(
    snackState: SnackbarHostState,
    onDoneClick: () -> Unit = {},
) {
    composable<ClaimNoteRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()

        ClaimNoteScreen(
            snackState = snackState,
            inheritanceAdditional = claimData.inheritanceAdditional ?: return@composable,
            onDoneClick = onDoneClick,
        )
    }
}

// Case C: shared ViewModel read + write
fun NavGraphBuilder.verifyInheritanceMessage(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onNavigateToExportComplete: () -> Unit = {},
) {
    composable<VerifyInheritanceMessageRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()
        val uiState by activityViewModel.uiState.collectAsStateWithLifecycle()

        if (claimData.challenge == null) return@composable  // guard missing required data

        VerifyInheritanceMessageScreen(
            snackState = snackState,
            claimData = claimData,
            sharedUiState = uiState,
            onBackPressed = onBackPressed,
            onSuccess = { result -> activityViewModel.updateSomething(result) },
            onNavigateToExportComplete = onNavigateToExportComplete,
            onEventHandled = activityViewModel::onEventHandled,
        )
    }
}
```

Rules:
- All callback parameters default to `{}` except required ones (like `onContinue` when it carries data)
- Access shared ViewModel via `hiltViewModel(viewModelStoreOwner = activity)` — never inject it into the screen composable directly
- Use guard `return@composable` when required shared data is null
- Route parameters are extracted with `it.toRoute<RouteType>()`

---

### Layer 3 — Screen ViewModel (`[ScreenName]ViewModel.kt`)

**Standard Hilt ViewModel (no screen-specific constructor params):**
```kotlin
@HiltViewModel
class ClaimBackupPasswordViewModel @Inject constructor(
    private val someUseCase: SomeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ClaimBackupPasswordUiState())
    val state: StateFlow<ClaimBackupPasswordUiState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<ClaimBackupPasswordEvent>()
    val event: SharedFlow<ClaimBackupPasswordEvent> = _event.asSharedFlow()

    fun doSomething() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            someUseCase(Unit).onSuccess { result ->
                _event.emit(ClaimBackupPasswordEvent.Success(result))
            }.onFailure { e ->
                _event.emit(ClaimBackupPasswordEvent.ShowError(e.message.orEmpty()))
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
}

data class ClaimBackupPasswordUiState(
    val isLoading: Boolean = false,
    val someData: String = "",
)

sealed class ClaimBackupPasswordEvent {
    data class ShowError(val message: String) : ClaimBackupPasswordEvent()
    data class Success(val result: SomeResult) : ClaimBackupPasswordEvent()
    data object NavigateBack : ClaimBackupPasswordEvent()
}
```

**Assisted Hilt ViewModel (screen-specific params needed at creation):**
```kotlin
@HiltViewModel(assistedFactory = VerifyInheritanceMessageViewModel.Factory::class)
class VerifyInheritanceMessageViewModel @AssistedInject constructor(
    private val signMessageUseCase: SignMessageUseCase,
    @Assisted private val signer: SignerModel,
    @Assisted private val challenge: SigningChallengeMessage,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(signer: SignerModel, challenge: SigningChallengeMessage): VerifyInheritanceMessageViewModel
    }

    // ... state and event same as above
}
```

Rules:
- Screen ViewModel events handle screen-local concerns (loading, errors, local navigation triggers)
- Cross-screen navigation is handled by callbacks passed from the nav function (not emitted as ViewModel events)
- Use `MutableSharedFlow` for events (fire-and-forget), `MutableStateFlow` for state (hold latest value)

---

### Layer 4 — Shared ViewModel Events (modify existing shared ViewModel)

When the screen needs to trigger navigation decisions that belong to the Activity level, add to the shared ViewModel's event sealed class and handle in the Activity's `LaunchedEffect`:

**In shared ViewModel event sealed class:**
```kotlin
sealed class ClaimInheritanceEvent {
    // ... existing events
    data object NewScreenDone : ClaimInheritanceEvent()
    data class NewScreenError(val errorCode: Int, val message: String) : ClaimInheritanceEvent()
}
```

**In shared ViewModel:**
```kotlin
fun triggerNewScreenNavigation() {
    viewModelScope.launch {
        _uiState.update { it.copy(event = ClaimInheritanceEvent.NewScreenDone) }
    }
}

fun onEventHandled() {
    _uiState.update { it.copy(event = null) }
}
```

**In Activity `LaunchedEffect`:**
```kotlin
LaunchedEffect(sharedUiState.event) {
    sharedUiState.event?.let { event ->
        when (event) {
            // ... existing cases
            is ClaimInheritanceEvent.NewScreenDone -> {
                navController.navigateToNextScreen()
            }
        }
        activityViewModel.onEventHandled()
    }
}
```

Rules:
- Always call `onEventHandled()` after processing every event
- Navigation decisions (which screen to go to) belong at the Activity level
- Screen-local UI decisions (show error snackbar, toggle loading) belong at screen level

---

### Layer 5 — Screen Composable (`[ScreenName]Screen.kt`)

#### 5a. Screen function (ViewModel + event wiring)

```kotlin
@Composable
fun ClaimMagicPhraseScreen(
    snackState: SnackbarHostState,
    viewModel: ClaimMagicPhraseViewModel = hiltViewModel(),
    onBackPressed: () -> Unit = {},
    onContinue: (String, InheritanceClaimingInit) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    if (state.isLoading) {
        NcLoadingDialog()
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            coroutineScope.launch {
                snackState.showNunchukSnackbar(
                    message = error,
                    type = NcToastType.ERROR,
                )
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is ClaimMagicPhraseEvent.ContinueSuccess -> {
                    onContinue(event.magic, event.initResult)
                }
            }
        }
    }

    ClaimMagicPhraseContent(
        modifier = Modifier,
        snackState = snackState,
        onBackPressed = onBackPressed,
        onContinue = viewModel::submit,
    )
}
```

**Snackbar triggering rules:**
- Errors stored in state (`state.error: String?`) → watch with `LaunchedEffect(state.error)`, launch via `rememberCoroutineScope()`, then clear with `viewModel.clearError()`
- One-shot events (navigate, show dialog) → collect with `collectLatest` inside `LaunchedEffect(Unit)`
- Never use `LaunchedEffect(Unit)` to show snackbars — the `LaunchedEffect` would not re-trigger if the same error fires twice
- Always call `viewModel.clearError()` after showing the snackbar so the state resets

**`showNunchukSnackbar` signature:**
```kotlin
// suspend extension on SnackbarHostState — must be called inside a coroutine
snackState.showNunchukSnackbar(
    message = "Something went wrong",
    type = NcToastType.ERROR,       // ERROR | SUCCESS | WARNING
    actionLabel = null,             // optional
    withDismissAction = false,      // optional
    duration = SnackbarDuration.Short  // optional
)
```

`NcToastType` values:
- `NcToastType.ERROR` — red background, used for failures
- `NcToastType.SUCCESS` — green background, used for confirmations
- `NcToastType.WARNING` — green background with warn icon

#### 5b. Content function (pure UI, no ViewModel)

The Content composable is the actual UI and must have no ViewModel dependency — this keeps it previewable.

```kotlin
@Composable
private fun ClaimMagicPhraseContent(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    magicalPhrase: String = "",
    onContinue: () -> Unit = {},
) {
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),   // always add navigationBarsPadding
        snackState = snackState,                       // always wire snackState here
        topBar = {
            NcImageAppBar(
                onBackPress = onBackPressed,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = stringResource(R.string.nc_text_continue),
                onClick = onContinue,
            )
        },
    ) { innerPadding ->
        // screen body using innerPadding
    }
}
```

**`NcScaffold` rules:**
- **Always use `NcScaffold` instead of raw `Scaffold`** — it automatically wires `NcSnackBarHost` to display styled toasts
- Pass `snackState` to `NcScaffold` — the snackbar host is managed inside `NcScaffold`; do NOT add a separate `SnackbarHost` call
- Always apply `.navigationBarsPadding()` on the `modifier` passed to `NcScaffold`
- Add `.imePadding()` on the modifier when the screen contains text input fields:
  ```kotlin
  NcScaffold(
      modifier = modifier.navigationBarsPadding().imePadding(),
      contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars),
      snackState = snackState,
      ...
  )
  ```
  The `contentWindowInsets` exclusion of `WindowInsets.statusBars` is required alongside `imePadding()` to prevent double-inset application

**`NcScaffold` full signature for reference:**
```kotlin
NcScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
)
```

#### 5c. Preview

```kotlin
@PreviewLightDark
@Composable
private fun ClaimMagicPhraseScreenPreview() {
    NunchukTheme {
        ClaimMagicPhraseContent(
            snackState = remember { SnackbarHostState() }
        )
    }
}
```

Rules:
- `hiltViewModel()` as default param on the Screen function — allows overriding in tests
- `snackState: SnackbarHostState` always the first param on both Screen and Content
- All user-facing callbacks (`onBackPressed`, `onContinue`) have default `{}` values
- Collect SharedFlow events with `collectLatest` inside `LaunchedEffect(Unit)`
- Error state from StateFlow uses `LaunchedEffect(state.error)` + `coroutineScope.launch`
- Separate `[ScreenName]Content` private composable with no VM dependency for Previews

---

### Layer 6 — Register in NavHost (modify Activity/Graph file)

In the `NavHost` builder inside the Activity or graph composable:

```kotlin
NavHost(navController = navController, startDestination = startScreen) {
    // ... existing destinations

    newScreen(                       // NavGraphBuilder extension
        snackState = snackbarHostState,
        onBackPressed = { navController.popBackStack() },
        onContinue = { data ->
            activityViewModel.handleData(data)
        },
    )
}
```

And in `LaunchedEffect(sharedUiState.event)`, add the new event case.

---

## File Naming & Package Convention

Given `ScreenName = "ClaimMagicPhrase"` and package `...claim`:

| File | Location |
|------|----------|
| `ClaimMagicPhraseRoute.kt` | `...claim/magicphrase/` |
| `ClaimMagicPhraseNavigation.kt` | `...claim/magicphrase/` |
| `ClaimMagicPhraseViewModel.kt` | `...claim/magicphrase/` |
| `ClaimMagicPhraseScreen.kt` | `...claim/magicphrase/` |

Sub-package name is the lowercase screen name stripped of the feature prefix (e.g., `magicphrase`, `addkey`, `claimnote`).

---

## Checklist Before Finishing

- [ ] Route object annotated with `@Serializable`
- [ ] `NavController` navigate extension created
- [ ] `NavGraphBuilder` extension created with proper shared ViewModel wiring
- [ ] Screen ViewModel uses `MutableStateFlow` for state, `MutableSharedFlow` for events
- [ ] Screen composable collects SharedFlow events via `collectLatest` in `LaunchedEffect(Unit)`
- [ ] Error state displayed via `LaunchedEffect(state.error)` + `rememberCoroutineScope().launch { snackState.showNunchukSnackbar(...) }`
- [ ] Content composable uses `NcScaffold` (not raw `Scaffold`) with `snackState` wired in
- [ ] `NcScaffold` modifier has `.navigationBarsPadding()` (+ `.imePadding()` and `contentWindowInsets` exclusion if text input is present)
- [ ] Preview wraps `Content` in `NunchukTheme` with `remember { SnackbarHostState() }`
- [ ] New destination registered in `NavHost` in the Activity/graph file
- [ ] New shared ViewModel events (if any) added to sealed class and handled in `LaunchedEffect`
- [ ] `onEventHandled()` called after every event consumption