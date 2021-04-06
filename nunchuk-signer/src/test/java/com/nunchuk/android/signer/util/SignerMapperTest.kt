package com.nunchuk.android.signer.util

internal class SignerMapperTest {

    @org.junit.Test
    fun parse() {
        val signerSpec =
            "[ABCD1234/48h/0h/2h]xpub-pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd"
        val signerInput = SignerMapper.toSigner(signerSpec)
        org.junit.Assert.assertEquals("ABCD1234", signerInput.fingerPrint)
        org.junit.Assert.assertEquals("m/48h/0h/2h", signerInput.path)
        org.junit.Assert.assertEquals(
            "pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd",
            signerInput.xpub
        )
    }

    @org.junit.Test
    fun parse1() {
        val signerSpec =
            "[ABCD1234/48h/0h/2h]xpub-pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd/1*/0*"
        val signerInput = SignerMapper.toSigner(signerSpec)
        org.junit.Assert.assertEquals("ABCD1234", signerInput.fingerPrint)
        org.junit.Assert.assertEquals("m/48h/0h/2h", signerInput.path)
        org.junit.Assert.assertEquals(
            "pub661MyMwAqRbcG8Zah6TcX3QpP5yJApaXcyLK8CJcZkuYjczivsHxVL5qm9cw8BYLYehgFeddK5WrxhntpcvqJKTVg96dUVL9P7hZ7Kcvqvd",
            signerInput.xpub
        )
    }

    @org.junit.Test
    fun parse2() {
        val signerSpec =
            "[ABCD1234/44'/1'/0'/0]xpub-xpub6EVnUYrCsgejjYzXeMcGV3sx6z3jAMKLsMqBzQ9MugqKM4jsEzVkSiRc5BrpSQ3JFzV2vYSqHs24itD1JvN3vEqWYTLi3QXkEyMGNd4eTSR"
        val signerInput = SignerMapper.toSigner(signerSpec)
        org.junit.Assert.assertEquals("ABCD1234", signerInput.fingerPrint)
        org.junit.Assert.assertEquals("m/44'/1'/0'/0", signerInput.path)
        org.junit.Assert.assertEquals(
            "xpub6EVnUYrCsgejjYzXeMcGV3sx6z3jAMKLsMqBzQ9MugqKM4jsEzVkSiRc5BrpSQ3JFzV2vYSqHs24itD1JvN3vEqWYTLi3QXkEyMGNd4eTSR",
            signerInput.xpub
        )
    }

}