package com.nunchuk.android.signer.util

import org.junit.Assert.*

internal class SignerMapperTest {

    @org.junit.Test
    fun parse() {
        val signerSpec =
            "[ABCD1234/48h/0h/2h]pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd"
        val signerInput = signerSpec.toSigner()
        assertEquals("ABCD1234", signerInput.fingerPrint)
        assertEquals("m/48h/0h/2h", signerInput.path)
        assertEquals(
            "pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd",
            signerInput.xpub
        )
    }

    @org.junit.Test
    fun parse1() {
        val signerSpec =
            "[ABCD1234/48h/0h/2h]pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd/1*/0*"
        val signerInput = signerSpec.toSigner()
        assertEquals("ABCD1234", signerInput.fingerPrint)
        assertEquals("m/48h/0h/2h", signerInput.path)
        assertEquals(
            "pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd",
            signerInput.xpub
        )
    }

    @org.junit.Test
    fun parse2() {
        val signerSpec =
            "[ABCD1234/44'/1'/0'/0]xpub6EVnUYrCsgejjYzXeMcGV3sx6z3jAMKLsMqBzQ9MugqKM4jsEzVkSiRc5BrpSQ3JFzV2vYSqHs24itD1JvN3vEqWYTLi3QXkEyMGNd4eTSR"
        val signerInput = signerSpec.toSigner()
        assertEquals("ABCD1234", signerInput.fingerPrint)
        assertEquals("m/44'/1'/0'/0", signerInput.path)
        assertEquals(
            "xpub6EVnUYrCsgejjYzXeMcGV3sx6z3jAMKLsMqBzQ9MugqKM4jsEzVkSiRc5BrpSQ3JFzV2vYSqHs24itD1JvN3vEqWYTLi3QXkEyMGNd4eTSR",
            signerInput.xpub
        )
    }

}