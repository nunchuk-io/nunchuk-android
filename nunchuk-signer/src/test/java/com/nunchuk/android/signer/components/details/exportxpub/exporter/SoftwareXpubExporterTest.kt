package com.nunchuk.android.signer.components.details.exportxpub.exporter

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoftwareXpubExporterTest {
    private val request = XpubRequest(
        masterSignerId = "master-id",
        masterFingerprint = "",
        derivationPath = "",
        path = "m/84h/0h/0h",
    )
    private val passphrase = XpubInput.Passphrase("secret")

    @Test
    fun `the passphrase is cleared after the export`() = runTest {
        val operations = FakeOperations().apply { derived.complete(Result.success("[xfp/84h/0h/0h]xpub")) }

        val step = SoftwareXpubExporter(operations).resume(request, passphrase)

        assertEquals(XpubStep.Done("[xfp/84h/0h/0h]xpub"), step)
        assertEquals(1, operations.clears)
    }

    @Test
    fun `the passphrase is cleared when sending it fails`() = runTest {
        val operations = FakeOperations(sendResult = Result.failure(IllegalStateException("send failed")))

        val step = SoftwareXpubExporter(operations).resume(request, passphrase)

        assertTrue(step is XpubStep.Failed)
        assertEquals(1, operations.clears)
    }

    @Test
    fun `the passphrase is cleared when the export is canceled midway`() = runTest(UnconfinedTestDispatcher()) {
        val operations = FakeOperations()
        val export = launch { SoftwareXpubExporter(operations).resume(request, passphrase) }

        export.cancel()

        assertEquals(1, operations.clears)
    }

    private class FakeOperations(
        private val sendResult: Result<Unit> = Result.success(Unit),
    ) : SoftwareKeyOperations {
        val derived = CompletableDeferred<Result<String>>()
        var clears = 0

        override suspend fun needsPassphrase(masterSignerId: String) = Result.success(true)

        override suspend fun sendPassphrase(masterSignerId: String, passphrase: String) = sendResult

        override suspend fun deriveDescriptor(masterSignerId: String, path: String) = derived.await()

        /** Suspends like the real SDK call, so it only runs on a canceled export if made non-cancelable. */
        override suspend fun clearPassphrase(masterSignerId: String): Result<Unit> {
            yield()
            clears++
            return Result.success(Unit)
        }
    }
}
