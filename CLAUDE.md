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

**Firebase App Distribution**: For normal tasks, always build/distribute the **Development Debug** variant. The upload task does NOT trigger assembly, so run both:
```bash
./gradlew :nunchuk-app:assembleDevelopmentDebug :nunchuk-app:appDistributionUploadDevelopmentDebug
```

**Prerequisites**: Build Nunchuk Android Native SDK from its separate repo, publish to local Maven, ensure `nativeSdkVersion` in `configs/dependencies.gradle` matches.

**Build config**: `configs/dependencies.gradle` (versions), `configs/submodule-config.gradle` (common module setup). Min SDK 24, Target 35, Compile 36, JVM 21, Kotlin 2.1.21.

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

**RULE — type-safe routes only**: every Compose destination MUST be a `@Serializable`
route object/class used with `composable<Route>` / `navigate(Route)`. Never use raw
string routes (`const val fooRoute = "foo"`, `composable("foo")`, `navigate("foo")`).
String-based routes are not type-safe, can't carry typed args, and are disallowed —
convert any you encounter. (`nunchuk.android.library`/`application` already apply the
`kotlin.plugin.serialization` plugin, so `@Serializable` is available in every module.)

**Route definition** (mark `internal` unless another module navigates to it):
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

### Native SDK Errors

Native calls (via `nunchuk-android-nativesdk`) throw `com.nunchuk.android.exception.NCNativeException` whose `message` is formatted `"<code>:<native message>"`. Use `Throwable.nativeErrorCode()` (`nunchuk-core/util/Util.kt`) to extract the numeric code — it returns the parsed `Int` for an `NCNativeException`, or `-1` otherwise.

Known codes live in `NativeErrorCode` (`nunchuk-core/.../constants/NativeErrorCode.kt`); the full set is defined in the native SDK's `NunchukException` (e.g. `INVALID_FEE_RATE = -1005` — fee rate below the minimum, `COIN_SELECTION_ERROR = -1011` — insufficient funds, `INVALID_AMOUNT = -1002`). Add a constant to `NativeErrorCode` when you need to branch on one.

**Convention**: don't surface the raw SDK message for cases with a designed message. Carry the code out of the ViewModel (e.g. an event field populated via `it.nativeErrorCode()`) and map the specific code to a `stringResource` in the UI, falling back to the raw SDK message for everything else. Example: `CustomizeLiquidFeeBottomSheet` validates a below-minimum fee client-side (shows `R.string.nc_input_fee_invalid_error`); an `INVALID_FEE_RATE` that still surfaces from the SDK at apply time maps to `R.string.nc_input_fee_insufficient` ("Insufficient fee"), and `event.message` is shown otherwise.

### Liquid / USDT wallet

Liquid wallets hold LBTC plus issued assets (notably USDT); the transaction **fee is always paid in LBTC**. Detect via `uiState.isLiquid` and compare an output's `assetId` against `usdtAssetId`.

**Reuse the shared amount formatters in `nunchuk-core/util/WalletUtil.kt`** — do NOT inline `pureBTC().formatDecimalWithoutZero(MAX_FRACTION_DIGITS) + " LBTC/USDT"` or redeclare a local `8`/`LIQUID_*_FRACTION_DIGITS` const (`MAX_FRACTION_DIGITS` already exists in `NumberFormatter.kt`):
- `Amount.getLbtcAmount()` — LBTC, honours the selected unit setting (sat / BTC / fixed precision)
- `Amount.getLbtcTokenAmount()` / `Amount.getUsdtTokenAmount()` — fixed-precision `"<amount> LBTC"` / `"<amount> USDT"`
- `Double.getLiquidTokenAmount(assetId, usdtAssetId)` — fixed-precision `"<amount> LBTC/USDT"`, symbol chosen by asset
- `Amount.getLiquidCurrencyAmount(assetId, usdtAssetId)` — fiat value (USDT ≈ 1:1, LBTC uses the BTC rate)

Liquid use cases (`nunchuk-domain`): `EstimateLiquidFeeUseCase` (fee rates incl. `minimumFee`), `EstimateFeeForLiquidTransactionUseCase`, `DraftUsdtTransactionUseCase`, `CreateUsdtTransactionUseCase`. The customize-fee UI is `CustomizeLiquidFeeBottomSheet` (input in LBTC).

### Migration: Fragment → Compose

The codebase is actively migrating from Fragment-based screens to Compose. New features use Compose navigation. Legacy screens (messages, some wallet screens) still use Fragments with XML navigation graphs. When adding new screens, always use Compose.

**Base activity hierarchy**:
- `BaseComposeActivity` — foundation: DI, locale, unauthorized handling, loading dialog
- `BaseActivity<Binding>` — adds ViewBinding support via `initializeBinding()`
- `BaseShareSaveFileActivity<Binding>` — adds file sharing/saving
- `BaseNfcActivity<Binding>` — adds NFC adapter, CVC input, scan dialogs
- `BaseComposeNfcActivity` — NFC support without ViewBinding (pure Compose)

## Signer Types & Key Flows

A key is identified by `SignerType` **plus** `SignerTag` — neither alone is enough. `SignerType.HARDWARE` covers Ledger, Trezor, BitBox and USB COLDCARD; the tag says which. Every flow below branches on that pair, so **any new key type has to be added to each dispatcher listed here** — a missed branch silently falls through to "use the desktop app" or does nothing at all.

| Key | `SignerType` | `SignerTag` |
|-----|--------------|-------------|
| TAPSIGNER | `NFC` | — |
| COLDCARD (NFC/QR/file) | `COLDCARD_NFC` | `COLDCARD` |
| COLDCARD (USB) | `HARDWARE` | `COLDCARD` |
| Portal | `PORTAL_NFC` | — |
| Ledger | `HARDWARE` | `LEDGER` |
| Trezor | `HARDWARE` | `TREZOR` |
| BitBox | `HARDWARE` | `BITBOX` |
| Jade / SeedSigner / Keystone / Foundation | `AIRGAP` | `JADE` / `SEEDSIGNER` / `KEYSTONE` / `PASSPORT` |
| Generic air-gapped | `AIRGAP` | none |
| Software (hot) key | `SOFTWARE`, `FOREIGN_SOFTWARE` | — |
| Server (platform) key | `SERVER` | — |

`SignerDisplayInfo.kt` (`nunchuk-signer`) is the single mapping between this pair and the UI-facing `KeyType` (`toKeyType()` / `toSignerTypeAndTag()` / `toDisplayInfo()`) — extend it there rather than re-deriving the pair per screen. `SignerUtil.kt` (`nunchuk-core/util/`) holds the shared helpers: `SignerTag?.isInAppHardwareTag` (true for Trezor + Ledger, i.e. the hardware keys that are not desktop-only), `SignerTag?.formattedName`, and the icon lookups.

### Add key (free / personal wallet)

Entry point `SignerIntroActivity` (`nunchuk-main/membership/signer/`) with `SignerIntroViewModel` (`nunchuk-signer`). The screen lists whatever the server reports as supported; tapping a card dispatches on `KeyType`:

- TAPSIGNER / COLDCARD / Portal → NFC setup (`navigateToSetupTapSigner`, `openSetupMk4`, `openPortalScreen`)
- Ledger / Trezor → in-app add-key activity (`LedgerActivity`, `TrezorActivity`) — BLE/USB, reads the xpub off the device
- BitBox → `handleHardwareSignerSelection` — desktop only
- Air-gapped tags → `handleSelectAddAirgapType` (QR/file import)
- Software → create/recover a hot key; Platform key → returned as a result

`LedgerViewModel.onXpubReceived` shows the naming convention for in-app hardware adds: membership flows auto-name (`Ledger`, `Ledger 2`, …), standalone add-key stops on "Name your key".

### Add key (assisted / membership)

`AddKeyListFragment` (personal) and `AddByzantineKeyListFragment` (group), both in `nunchuk-main/membership/`. Options come from a `BottomSheetOption`, and every branch funnels through `handleShowKeysOrCreate(existingSigners, type) { createNew() }` — if the app already holds matching keys it opens `TapSignerListBottomSheetFragment` to reuse one, otherwise it runs the create lambda.

Hardware keys split by tag (`openInAppHardwareOrDesktopFlow`): **Trezor and Ledger have in-app flows; every other hardware key (BitBox, COLDCARD via USB) goes to `AddDesktopKeyFragment`**, which asks the user to finish in the desktop app and waits for the key to arrive from the server (`RequestAddKeySuccessFragment`). `AddDesktopKeyFragment` renders copy for the `COLDCARD` / `TREZOR` / `LEDGER` / `BITBOX` / `JADE` tags.

The on-chain timelock variants (`OnChainTimelockAddKeyListFragment`, `OnChainReplaceKeysFragment`) deliberately route **all** hardware tags — Ledger and Trezor included — to `openRequestAddDesktopKey`.

### Replace key

`ReplaceKeysFragment` (`nunchuk-main/membership/replacekey/`) mirrors the assisted add-key option sheet one-for-one; the difference is that every intent carries `replacedXfp` plus `walletId`/`groupId`, and the desktop path is `showAddKeyByDesktopApp()`. Keep the two in sync — a key type added to assisted add-key but not here is un-replaceable.

### Signing

Two dispatchers, one per host, each a `when` over the type/tag pair. They are **not** shared code, so a new key type needs both:

| Key | Normal tx (`TransactionDetailComposeActivity.onSignClick` + `TransactionDetailsViewModel`) | Dummy tx, incl. sign-in (`WalletAuthenticationViewModel.onSignerSelect`) |
|-----|------------------|-----------------|
| TAPSIGNER | NFC scan | NFC scan |
| COLDCARD | NFC / QR / file export + import signature | same |
| Portal | `handlePortalAction(SignTransaction)` | `RequestSignPortal` |
| Ledger | `LedgerSignTransactionSheet` (signs + imports PSBT) | `LedgerSignPsbtSheet(walletId)`, or `LedgerSignPsbtSheet(wallet)` at sign-in |
| Trezor | deeplink to Trezor Suite + callback | same |
| BitBox / other `HARDWARE` | "use the desktop app" | same |
| Air-gapped | export/import PSBT via QR/file | same |
| Software | `handleSignSoftwareKey` (passphrase prompt if needed) | `checkSoftwarePassPhrase` |
| Server | no Sign action — the server co-signs | n/a |

**Sign-in via digital signature runs on the dummy-tx dispatcher**, not on a host of its own: `EnterXPUBActivity` → `openWalletAuthentication(walletId = "", type = SIGN_DUMMY_TX, signatureFlowType = SIGN_IN)` → `WalletAuthenticationActivity` → `DummyTransactionIntroFragment` → `DummyTransactionDetailsFragment`. `isSignInSignatureFlow` is what varies inside that one view model: the dummy tx comes from `GetSignInDummyTransactionUseCase`, no wallet is loaded from the local DB (`args.walletId` is blank), TAPSIGNER goes through `CheckSignMessageTapsignerSignInUseCase`, and signatures upload via `uploadSignatureForSignIn`. Anything keyed off `args.walletId` being present therefore breaks sign-in — that is how Ledger keys ended up on "use the desktop app".

The `SignInAuthentication*` / `SignInDummyTransaction*` screens in `nunchuk-auth` look like that host but are **dead code**: `SignInAuthenticationActivity.start()` has no callers, and its nav graph is inflated only by itself. Don't fix sign-in bugs there.

Shared details:

- **Dummy transactions** (membership dummy tx, sign-in dummy tx, and `CheckSignMessageFragment`) converge on `handleSignatureResult` → upload. PSBT-based signers (Ledger, Trezor, COLDCARD, air-gapped) get there via `GetDummyTransactionSignatureUseCase` — signed PSBT in, signature out; TAPSIGNER signs the message directly (`CheckSignMessageTapsignerUseCase`, `CheckSignMessageTapsignerSignInUseCase` at login). A new PSBT-producing signer should reuse `GetDummyTransactionSignatureUseCase` rather than inventing a second path.
- **Trezor** signs out-of-app: `GetTrezorSignTransactionDeeplinkUseCase` → Trezor Suite → `TrezorCallbackHolder` → `ParseTrezorSignTransactionResponseUseCase`. It needs a `Wallet`, which at sign-in is parsed from the BSMS (`resolveWallet`).
- **Ledger** signs in-app over BLE/USB through `LedgerSheet.kt` (`nunchuk-core/ledger/`) → `LedgerSheetViewModel` → `LedgerTransactionSigner`. Signing always registers the wallet policy on the device first (`LedgerWalletRegistrar`); the registration HMAC is cached per local wallet, so a wallet that has no local storage (sign-in) passes `cacheRegistration = false` and re-registers every time.
- The fall-through `signerModel.type == SignerType.HARDWARE -> CanNotSignHardwareKey` ("Please use the desktop app to sign with this key") sits **after** the tag checks in both dispatchers. A new hardware tag added without its own branch lands there silently.

## Conventions

- **Reuse, don't repeat (DRY)**: before writing a formatter, extension, mapper, or constant, search for an existing one (shared utils live in `nunchuk-core/util/`, e.g. `WalletUtil.kt`, `NumberFormatter.kt`; reusable UI in `nunchuk-core/.../compose/`). If the same logic appears in 2+ places, extract it to the nearest shared module instead of copy-pasting or redeclaring local constants. Prefer calling a shared method over inlining its body.
- Package root: `com.nunchuk.android`
- Logging: `Timber` (never `Log.d`)
- Error messages: `.orUnknownError()` extension for null-safe error strings
- State: always `collectAsStateWithLifecycle()` (never `collectAsState()`)
- Nc-prefixed components: reusable Compose components in `nunchuk-core/.../compose/` (e.g., `NcTextField`, `NcTopAppBar`, `NcScaffold`, `NcPrimaryDarkButton`, `NcLoadingDialog`, `NcConfirmationDialog`, `NcHintMessage`, `NcBadge`, `NcTag`, `NcSwitch`, `NcCheckBox`, `NcRadioOption`)
- Fonts: Montserrat Medium (headings), Lato Regular/Bold (body)
- Dark mode: fully supported via `NunchukTheme` + `ThemeManager`
