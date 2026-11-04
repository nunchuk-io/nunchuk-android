package com.nunchuk.android.core.matrix

import org.junit.Assert.assertEquals
import org.junit.Test
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import java.io.IOException
import java.net.SocketTimeoutException

class MatrixFailureCategoryTest {

    private fun serverError(code: String, http: Int) =
        Failure.ServerError(MatrixError(code = code, message = ""), http)

    @Test
    fun `network and timeouts are transient`() {
        assertEquals(MatrixFailureCategory.TRANSIENT, Failure.NetworkConnection(IOException()).matrixFailureCategory())
        assertEquals(MatrixFailureCategory.TRANSIENT, SocketTimeoutException().matrixFailureCategory())
    }

    @Test
    fun `server 5xx and rate limit are transient`() {
        assertEquals(MatrixFailureCategory.TRANSIENT, serverError(MatrixError.M_UNKNOWN, 503).matrixFailureCategory())
        assertEquals(MatrixFailureCategory.TRANSIENT, serverError(MatrixError.M_LIMIT_EXCEEDED, 429).matrixFailureCategory())
    }

    @Test
    fun `rejected credentials are not retried`() {
        assertEquals(MatrixFailureCategory.INVALID_CREDENTIALS, serverError(MatrixError.M_UNKNOWN_TOKEN, 401).matrixFailureCategory())
        assertEquals(MatrixFailureCategory.INVALID_CREDENTIALS, serverError(MatrixError.M_FORBIDDEN, 403).matrixFailureCategory())
    }

    @Test
    fun `deactivated account is an account rejection`() {
        assertEquals(MatrixFailureCategory.ACCOUNT_REJECTED, serverError(MatrixError.M_USER_DEACTIVATED, 403).matrixFailureCategory())
    }

    @Test
    fun `local errors are not treated as rejected credentials`() {
        assertEquals(MatrixFailureCategory.UNEXPECTED, IllegalStateException("main thread").matrixFailureCategory())
    }
}
