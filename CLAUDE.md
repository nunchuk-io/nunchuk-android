# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development

Multi-module Android app (Nunchuk wallet) using Gradle with mixed Kotlin DSL and Groovy scripts.

```bash
./gradlew assembleDebug              # Build debug APK
./gradlew installDebug               # Install on device
./gradlew test                       # Run all unit tests
./gradlew :nunchuk-main:testDebugUnitTest  # Single module tests
./gradlew lint                       # Lint
```

**Prerequisites**: Build Nunchuk Android Native SDK from its separate repo, publish to local Maven, ensure `nativeSdkVersion` in `configs/dependencies.gradle` matches.

**Build config**: `configs/dependencies.gradle` (versions), `configs/submodule-config.gradle` (common module setup). Min SDK 24, Target 35, Compile 36, JVM 17, Kotlin 2.1.21.

## Module Layout

| Module | Purpose |
|--------|---------|
| `nunchuk-app` | Application entry point, top-level DI |
| `nunchuk-domain` | Domain models, repository interfaces, use cases |
| `nunchuk-core` | Base classes, repository implementations, NFC support, theme |
| `nunchuk-arch` | Legacy `NunchukViewModel<State, Event>` base class |
| `nunchuk-main` | Primary feature screens (wallets, inheritance, services) |
| `nunchuk-network` | Retrofit API definitions |
| `nunchuk-database` | Room database and DAOs |
| `nunchuk-widget` | Shared UI components (Nc-prefixed Compose components live in `nunchuk-core/compose/`) |
| `nunchuk-wallet*` | Wallet features (core, personal, shared) |
| `nunchuk-signer*` | Signer management |
| `nunchuk-transaction` | Transaction handling |
| `nunchuk-auth` | Authentication flows |
| `nunchuk-contact` | Contact management |
| `nunchuk-messages` | Chat / messaging (Matrix SDK) |
| `nunchuk-settings` | App settings screens |
| `nunchuk-notifications` | Push notification handling |
| `nunchuk-utils` | Kotlin extension utilities (e.g., `Flow.onException`) |

## Architecture Rules

### Use Case Layer (`nunchuk-domain`)

Use cases extend `UseCase<P, R>` and return `Result<R>` via `suspend operator fun invoke()`.

```kotlin
class MyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: MyRepository,
) : UseCase<MyUseCase.Param, MyResult>(dispatcher) {
    override suspend fun execute(parameters: Param): MyResult { ... }

    data class Param(val walletId: String, val amount: Long)
}
```

- **No params**: use `Unit` as `P`
- **Single/multiple params**: define nested `data class Param(...)` inside the use case
- Always inject `@IoDispatcher` dispatcher
- Never throw from `execute()`; the base class wraps in `Result`

### Repository Layer

- **Interface** in `nunchuk-domain/repository/` — defines contract
- **Implementation** in `nunchuk-core/repository/` or feature module — annotated with `@Inject constructor`
- **Binding** via Hilt `@Module` with `@Binds @Singleton` in `/di/` directories

```kotlin
// nunchuk-domain
interface MyRepository {
    suspend fun getData(id: String): MyModel
    fun observeData(id: String): Flow<MyModel>
}

// nunchuk-core/di/DataModule.kt
@Binds @Singleton
fun bindMyRepository(impl: MyRepositoryImpl): MyRepository
```

### ViewModel Layer

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val myUseCase: MyUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(MyUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<MyEvent>()
    val event = _event.asSharedFlow()

    fun doAction() = viewModelScope.launch {
        _event.emit(MyEvent.Loading(true))
        myUseCase(MyUseCase.Param(...))
            .onSuccess { data ->
                _state.update { it.copy(data = data) }
            }.onFailure {
                _event.emit(MyEvent.Error(it.message.orUnknownError()))
            }
        _event.emit(MyEvent.Loading(false))
    }

    fun updateField(value: String) = _state.update { it.copy(field = value) }
}
```

**State rules**:
- `_state: MutableStateFlow<UiState>` — reactive UI state, `data class` with defaults
- `_event: MutableSharedFlow<Event>` — one-shot events (navigation, errors, snackbar), `sealed class`
- State mutations via `_state.update { it.copy(...) }`
- Events via `viewModelScope.launch { _event.emit(...) }`
- `MutableSaveStateFlow(savedStateHandle, key, defaultValue)` — only for data that must survive process death (requires `@Parcelize`)

**Error handling** — chain `onSuccess`/`onFailure` directly on the use case call (no intermediate variable):
```kotlin
myUseCase(MyUseCase.Param(...))
    .onSuccess { data -> _state.update { it.copy(data = data) } }
    .onFailure { _event.emit(MyEvent.Error(it.message.orUnknownError())) }
```

Note: `Flow.onException` (from `nunchuk-utils`) is for Flow error handling, not Result.

### Compose UI Layer

**File organization per screen**:
```
FeatureScreen.kt          — Screen + Content composables + Preview
FeatureNavigation.kt      — @Serializable route, NavGraphBuilder ext, NavController ext
FeatureViewModel.kt       — @HiltViewModel
```

**Screen composable** (has ViewModel, collects state):
```kotlin
@Composable
internal fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    onBackClicked: () -> Unit = {},
    onSuccess: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MyContent(
        data = state.data,
        onFieldChanged = viewModel::updateField,
        onContinueClicked = { viewModel.doAction() },
        onBackClicked = onBackClicked,
    )
}
```

**Content composable** (stateless, previewable):
```kotlin
@Composable
private fun MyContent(
    data: String = "",
    onFieldChanged: (String) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onBackClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = { NcTopAppBar(title = "...", onBackPress = onBackClicked) },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    onClick = onContinueClicked,
                ) { Text(text = stringResource(R.string.nc_text_continue)) }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
            ) {
                Text(text = "Title", style = NunchukTheme.typography.heading)
                Text(text = "Body", style = NunchukTheme.typography.body)
                NcTextField(value = data, onValueChange = onFieldChanged)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MyContentPreview() { MyContent() }
```

### Compose Navigation

**Route definition**:
```kotlin
@Serializable data object MyScreenRoute                          // no args
@Serializable data class MyScreenRoute(val id: String = "")      // with args (primitives only)
```

**NavGraphBuilder extension** (registers destination):
```kotlin
fun NavGraphBuilder.myScreen(
    onBackClicked: () -> Unit = {},
    onContinueClicked: (String) -> Unit = {},
) {
    composable<MyScreenRoute> {
        MyScreen(onBackClicked = onBackClicked, onContinueClicked = onContinueClicked)
    }
}
```

**NavController extension** (navigation action):
```kotlin
fun NavController.navigateToMyScreen(id: String = "") {
    navigate(MyScreenRoute(id = id))
}
```

**Activity-level NavHost** (hosts the flow, handles shared state):
```kotlin
@AndroidEntryPoint
class MyActivity : BaseNfcActivity<ViewBinding>() {
    private val viewModel by viewModels<MyFlowViewModel>()

    override fun initializeBinding(): ViewBinding = ViewBinding {
        ComposeView(this).apply {
            setContent { MyGraph(activity = this@MyActivity, activityViewModel = viewModel) }
        }
    }.also { enableEdgeToEdge() }
}

@Composable
private fun MyGraph(activity: MyActivity, activityViewModel: MyFlowViewModel) {
    val navController = rememberNavController()
    val sharedState by activityViewModel.state.collectAsStateWithLifecycle()

    // Handle one-shot events from shared ViewModel
    LaunchedEffect(Unit) {
        activityViewModel.event.collect { event ->
            when (event) {
                is MyEvent.Error -> { /* show snackbar */ }
                is MyEvent.NavigateNext -> navController.navigateToNextScreen()
            }
        }
    }

    NunchukTheme {
        NavHost(navController = navController, startDestination = FirstScreenRoute) {
            firstScreen(onContinueClicked = { navController.navigateToNextScreen() })
            nextScreen(onBackClicked = { navController.popBackStack() })
        }
    }
}
```

**Accessing activity-scoped ViewModel from a destination**:
```kotlin
composable<MyRoute> {
    val activity = LocalActivity.current as ComponentActivity
    val activityViewModel: MyFlowViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val sharedData by activityViewModel.data.collectAsStateWithLifecycle()
    MyScreen(sharedData = sharedData)
}
```

### UI Components & Styling

- **Theme**: wrap all screens in `NunchukTheme { }`
- **Typography**: `NunchukTheme.typography.heading`, `.title`, `.titleSmall`, `.body`, `.bodySmall`, `.bold`, `.caption`
- **Colors**: `MaterialTheme.colorScheme.textPrimary`, `.textSecondary`, `.strokePrimary`
- **Scaffold**: use `NcScaffold` (with snackbar) or `Scaffold` (Material3)
- **Buttons**: `NcPrimaryDarkButton`, `NcOutlineButton`
- **Text fields**: `NcTextField`
- **Loading**: `NcLoadingDialog()`
- **Snackbar**: `snackState.showNunchukSnackbar(message, type = NcToastType.ERROR)`
- **Strings**: `stringResource(R.string.nc_xxx)` in Compose, `context.getString(R.string.nc_xxx)` in ViewModel
- **Previews**: always add `@PreviewLightDark` on content composables

### DI (Hilt)

- Activities: `@AndroidEntryPoint`
- ViewModels: `@HiltViewModel` with `@Inject constructor`
- Dispatchers: `@IoDispatcher`, `@MainDispatcher`, `@DefaultDispatcher`, `@MainImmediateDispatcher`
- In Compose: `hiltViewModel()` for screen-scoped, `hiltViewModel(viewModelStoreOwner = activity)` for activity-scoped

### Data Models

- `@Parcelize` data classes for Intent extras and `SavedStateHandle`
- `@Serializable` data objects/classes for Compose Navigation routes (primitives only, no complex objects)
- Domain models live in `nunchuk-domain/model/`

### Cross-Module Navigation

Modules cannot depend on each other directly. Cross-module navigation uses `NunchukNavigator` (interface in `nunchuk-core/nav/`, implementation in `nunchuk-app/nav/NunchukNavigatorImpl.kt`). It composes sub-interfaces: `AppNavigator`, `AuthNavigator`, `WalletNavigator`, `SignerNavigator`, `TransactionNavigator`, `MessageNavigator`, `ContactNavigator`, `SettingNavigator`, `NfcNavigator`, `MainNavigator`.

Activities use `ActivityArgs` (interface in `nunchuk-arch/args/`) for type-safe intent extras:
```kotlin
data class MyArgs(val id: String) : ActivityArgs {
    override fun buildIntent(activityContext: Context) = Intent(activityContext, MyActivity::class.java).apply {
        putExtra(EXTRA_ID, id)
    }
    companion object {
        fun deserializeFrom(intent: Intent) = MyArgs(intent.getStringExtra(EXTRA_ID).orEmpty())
    }
}
```

### Network Layer

APIs return `Data<T>` wrapper (`nunchuk-network`). The wrapper auto-throws `NunchukApiException` on error codes and publishes `UnauthorizedEventBus` on 401s.

```kotlin
interface MyApi {
    @GET("/v1.1/my/endpoint")
    suspend fun getData(): Data<MyResponse>
}
```

### Migration: Fragment → Compose

The codebase is actively migrating from Fragment-based screens to Compose. New features use Compose navigation. Legacy screens (messages, some wallet screens) still use Fragments with XML navigation graphs. When adding new screens, always use Compose.

**Base activity hierarchy**:
- `BaseComposeActivity` — foundation: DI, locale, unauthorized handling, loading dialog
- `BaseActivity<Binding>` — adds ViewBinding support via `initializeBinding()`
- `BaseShareSaveFileActivity<Binding>` — adds file sharing/saving
- `BaseNfcActivity<Binding>` — adds NFC adapter, CVC input, scan dialogs
- `BaseComposeNfcActivity` — NFC support without ViewBinding (pure Compose)

## Conventions

- Package root: `com.nunchuk.android`
- Logging: `Timber` (never `Log.d`)
- Error messages: `.orUnknownError()` extension for null-safe error strings
- State: always `collectAsStateWithLifecycle()` (never `collectAsState()`)
- Nc-prefixed components: reusable Compose components in `nunchuk-core/.../compose/` (e.g., `NcTextField`, `NcTopAppBar`, `NcScaffold`, `NcPrimaryDarkButton`, `NcLoadingDialog`, `NcConfirmationDialog`, `NcHintMessage`, `NcBadge`, `NcTag`, `NcSwitch`, `NcCheckBox`, `NcRadioOption`)
- Fonts: Montserrat Medium (headings), Lato Regular/Bold (body)
- Dark mode: fully supported via `NunchukTheme` + `ThemeManager`
