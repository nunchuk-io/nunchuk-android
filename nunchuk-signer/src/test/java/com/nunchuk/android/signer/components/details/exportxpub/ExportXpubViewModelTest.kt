package com.nunchuk.android.signer.components.details.exportxpub

import androidx.lifecycle.SavedStateHandle
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubAction
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubExporter
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubExporters
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubInput
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubKey
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubPathRules
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubRequest
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubStep
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportXpubViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val args = ExportXpubArgs(
        isMasterSigner = true,
        masterSignerId = "master-id",
        signerType = SignerType.NFC,
        signerTags = emptyList(),
        masterFingerprint = "",
        derivationPath = "",
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `resubmitting the path that is still exporting is ignored`() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val viewModel = viewModel(exporter)
        val events = collectEvents(viewModel)

        viewModel.onCustomPathChanged(PATH_A)
        viewModel.exportCustomPath()
        viewModel.exportCustomPath()
        exporter.answer(PATH_A, XpubStep.Done(DESCRIPTOR_A))

        assertEquals(listOf(PATH_A), exporter.exported)
        assertEquals(1, events.count { it == ExportXpubEvent.OpenExport })
    }

    @Test
    fun `a newer path replaces the running export and the stale answer is dropped`() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val viewModel = viewModel(exporter)
        val events = collectEvents(viewModel)

        viewModel.onCustomPathChanged(PATH_A)
        viewModel.exportCustomPath()
        viewModel.onCustomPathChanged(PATH_B)
        viewModel.exportCustomPath()
        exporter.answer(PATH_A, XpubStep.Done(DESCRIPTOR_A))
        assertEquals(ExportXpubLoading.DEFAULT, viewModel.state.value.loading)

        exporter.answer(PATH_B, XpubStep.Done(DESCRIPTOR_B))

        assertEquals(DESCRIPTOR_B, viewModel.state.value.descriptor)
        assertEquals(1, events.count { it == ExportXpubEvent.OpenExport })
        assertEquals(ExportXpubLoading.NONE, viewModel.state.value.loading)
    }

    @Test
    fun `a canceled export never reaches the screen even when the SDK answers later`() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val viewModel = viewModel(exporter)
        val events = collectEvents(viewModel)

        viewModel.onCustomPathChanged(PATH_A)
        viewModel.exportCustomPath()
        viewModel.cancelExport()
        assertEquals(ExportXpubLoading.NONE, viewModel.state.value.loading)

        exporter.answer(PATH_A, XpubStep.Done(DESCRIPTOR_A))

        assertEquals("", viewModel.state.value.descriptor)
        assertTrue(events.none { it == ExportXpubEvent.OpenExport })
    }

    @Test
    fun `the custom path draft survives process death and is checked again`() = runTest(dispatcher) {
        val savedState = SavedStateHandle(args.toSavedState())
        viewModel(FakeExporter(), savedState).onCustomPathChanged(PATH_A)

        val restored = viewModel(FakeExporter(), savedState)

        assertEquals(PATH_A, restored.state.value.customPath)
        assertEquals(true, restored.state.value.isCustomPathValid)
    }

    @Test
    fun `a restored card prompt continues the path that asked for it`() = runTest(dispatcher) {
        val savedState = SavedStateHandle(args.toSavedState())
        val exporter = FakeExporter()
        val viewModel = viewModel(exporter, savedState)
        val events = collectEvents(viewModel)
        viewModel.onCustomPathChanged(PATH_A)
        viewModel.exportCustomPath()
        exporter.answer(PATH_A, XpubStep.NeedInput(XpubAction.NfcTap))
        assertTrue(events.contains(ExportXpubEvent.NeedInput(XpubAction.NfcTap)))

        val restoredExporter = FakeExporter()
        val restored = viewModel(restoredExporter, savedState)
        restored.onInput(PASSPHRASE)

        assertEquals(listOf(PATH_A to PASSPHRASE), restoredExporter.resumed)
        assertEquals(PATH_A, restored.state.value.customPath)
    }

    @Test
    fun `declining the prompt forgets the path it was for`() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val viewModel = viewModel(exporter)
        viewModel.onCustomPathChanged(PATH_A)
        viewModel.exportCustomPath()
        exporter.answer(PATH_A, XpubStep.NeedInput(XpubAction.NfcTap))

        viewModel.onInputCanceled()
        viewModel.onInput(PASSPHRASE)

        assertTrue(exporter.resumed.isEmpty())
        assertNull(viewModel.state.value.descriptor.takeIf { it.isNotEmpty() })
    }

    @Test
    fun `pressing Continue twice resolves the preset once and a cancel holds after it resolves`() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val pathRules = DelayedPathRules()
        val viewModel = viewModel(exporter, pathRules = pathRules)

        viewModel.exportPreset(isSingleSig = true, addressType = AddressType.NATIVE_SEGWIT, accountIndex = 0)
        viewModel.exportPreset(isSingleSig = true, addressType = AddressType.NATIVE_SEGWIT, accountIndex = 0)
        viewModel.cancelExport()
        pathRules.answer(0)

        assertEquals(listOf(0), pathRules.requested)
        assertTrue(exporter.exported.isEmpty())
        assertEquals(ExportXpubLoading.NONE, viewModel.state.value.loading)
    }

    @Test
    fun `canceling while the preset path resolves never starts the export`() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val pathRules = DelayedPathRules()
        val viewModel = viewModel(exporter, pathRules = pathRules)
        val events = collectEvents(viewModel)

        viewModel.exportPreset(isSingleSig = true, addressType = AddressType.NATIVE_SEGWIT, accountIndex = 0)
        assertEquals(ExportXpubLoading.DEFAULT, viewModel.state.value.loading)
        viewModel.cancelExport()
        pathRules.answer(0)

        assertTrue(exporter.exported.isEmpty())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `preset paths answered out of order export only the latest selection`() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val pathRules = DelayedPathRules()
        val viewModel = viewModel(exporter, pathRules = pathRules)

        viewModel.exportPreset(isSingleSig = true, addressType = AddressType.NATIVE_SEGWIT, accountIndex = 0)
        viewModel.exportPreset(isSingleSig = true, addressType = AddressType.NATIVE_SEGWIT, accountIndex = 1)
        pathRules.answer(1)
        pathRules.answer(0)

        assertEquals(listOf(DelayedPathRules.pathFor(1)), exporter.exported)
    }

    @Test
    fun `a late preset path cannot replace a custom path export`() = runTest(dispatcher) {
        val exporter = FakeExporter()
        val pathRules = DelayedPathRules()
        val viewModel = viewModel(exporter, pathRules = pathRules)

        viewModel.exportPreset(isSingleSig = true, addressType = AddressType.NATIVE_SEGWIT, accountIndex = 0)
        viewModel.onCustomPathChanged(PATH_B)
        viewModel.exportCustomPath()
        pathRules.answer(0)

        assertEquals(listOf(PATH_B), exporter.exported)
    }

    @Test
    fun `a stored key whose spec Key info passed opens complete without loading`() = runTest(dispatcher) {
        val exporter = FakeExporter(selectsAccount = false)
        val storedKey = args.copy(isMasterSigner = false, descriptor = DESCRIPTOR_A)

        val viewModel = viewModel(exporter, savedState = SavedStateHandle(storedKey.toSavedState()))

        assertEquals(DESCRIPTOR_A, viewModel.state.value.descriptor)
        assertEquals(ExportXpubLoading.NONE, viewModel.state.value.loading)
        assertTrue(exporter.exported.isEmpty())
    }

    @Test
    fun `canceling a stored key while its spec loads returns to Key info`() = runTest(dispatcher) {
        val exporter = FakeExporter(selectsAccount = false)
        val viewModel = viewModel(exporter, savedState = SavedStateHandle(args.copy(isMasterSigner = false).toSavedState()))
        val events = collectEvents(viewModel)
        assertEquals(listOf<String?>(null), exporter.exported)

        viewModel.cancelExport()
        exporter.answer(null, XpubStep.Done(DESCRIPTOR_A))

        assertEquals(listOf<ExportXpubEvent>(ExportXpubEvent.Close), events)
        assertEquals("", viewModel.state.value.descriptor)
    }

    private fun viewModel(
        exporter: XpubExporter,
        savedState: SavedStateHandle = SavedStateHandle(args.toSavedState()),
        pathRules: XpubPathRules = FakePathRules,
    ) = ExportXpubViewModel(savedState, XpubExporters(setOf(exporter)), pathRules)

    private fun TestScope.collectEvents(viewModel: ExportXpubViewModel): List<ExportXpubEvent> {
        val events = mutableListOf<ExportXpubEvent>()
        backgroundScope.launch { viewModel.event.collect { events += it } }
        return events
    }

    /** Answers each path when the test says so, and — like the SDK — even after being canceled. */
    private class FakeExporter(override val selectsAccount: Boolean = true) : XpubExporter {
        val exported = mutableListOf<String?>()
        val resumed = mutableListOf<Pair<String?, XpubInput>>()
        private val answers = mutableMapOf<String?, CompletableDeferred<XpubStep>>()

        fun answer(path: String?, step: XpubStep) {
            answers.getOrPut(path) { CompletableDeferred() }.complete(step)
        }

        override fun supports(key: XpubKey) = true

        override suspend fun export(request: XpubRequest): XpubStep {
            exported += request.path
            return awaitAnswer(request.path)
        }

        override suspend fun resume(request: XpubRequest, input: XpubInput): XpubStep {
            resumed += request.path to input
            return awaitAnswer(request.path)
        }

        private suspend fun awaitAnswer(path: String?) = withContext(NonCancellable) {
            answers.getOrPut(path) { CompletableDeferred() }.await()
        }
    }

    /** Resolves each account's preset path when the test says so, even after being canceled. */
    private class DelayedPathRules : XpubPathRules {
        val requested = mutableListOf<Int>()
        private val answers = mutableMapOf<Int, CompletableDeferred<String>>()

        fun answer(accountIndex: Int) {
            answers.getOrPut(accountIndex) { CompletableDeferred() }.complete(pathFor(accountIndex))
        }

        override suspend fun presetPath(isSingleSig: Boolean, addressType: AddressType, accountIndex: Int): Result<String> {
            requested += accountIndex
            return withContext(NonCancellable) {
                Result.success(answers.getOrPut(accountIndex) { CompletableDeferred() }.await())
            }
        }

        override suspend fun isValid(path: String) = path.startsWith("m/")

        companion object {
            fun pathFor(accountIndex: Int) = "m/84h/0h/${accountIndex}h"
        }
    }

    private object FakePathRules : XpubPathRules {
        override suspend fun presetPath(isSingleSig: Boolean, addressType: AddressType, accountIndex: Int) =
            Result.success("m/84h/0h/${accountIndex}h")

        override suspend fun isValid(path: String) = path.startsWith("m/")
    }

    private companion object {
        const val PATH_A = "m/84h/0h/0h"
        const val PATH_B = "m/48h/0h/0h/2h"
        const val DESCRIPTOR_A = "[3442193e/84h/0h/0h]xpubA"
        const val DESCRIPTOR_B = "[3442193e/48h/0h/0h/2h]xpubB"
        val PASSPHRASE = XpubInput.Passphrase("secret")
    }
}
