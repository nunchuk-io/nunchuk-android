package com.nunchuk.android.signer.util

import com.nunchuk.android.core.signer.toSigner
import org.junit.Assert.assertEquals
import org.junit.Test

internal class SignerMapperTest {

    @Test
    fun parse() {
        val signerInput = "[abcd1234/48h/0h/2h]pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd".toSigner()
        assertEquals("abcd1234", signerInput.fingerPrint)
        assertEquals("m/48h/0h/2h", signerInput.derivationPath)
        assertEquals(
            "pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd",
            signerInput.xpub
        )
    }

    @Test
    fun toSignerWithSpace() {
        val signerInput = "[abcd1234/48h/0h/2h]pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd ".toSigner()
        assertEquals("abcd1234", signerInput.fingerPrint)
        assertEquals("m/48h/0h/2h", signerInput.derivationPath)
        assertEquals(
            "pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd",
            signerInput.xpub
        )
    }

    @Test
    fun toSignerWithLineBreak() {
        val signerInput = "[abcd1234/48h/0h/2h]pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd\n".toSigner()
        assertEquals("abcd1234", signerInput.fingerPrint)
        assertEquals("m/48h/0h/2h", signerInput.derivationPath)
        assertEquals(
            "pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd",
            signerInput.xpub
        )
    }

    @Test
    fun toSignerExtra() {
        val signerInput = "[abcd1234/48h/0h/2h]pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd/1*/0*".toSigner()
        assertEquals("abcd1234", signerInput.fingerPrint)
        assertEquals("m/48h/0h/2h", signerInput.derivationPath)
        assertEquals(
            "pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd",
            signerInput.xpub
        )
    }

}