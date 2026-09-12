package com.nunchuk.android.core.util

import androidx.annotation.Keep

/** Which screen of the seed-phrase backup flow the caller wants to start at. */
@Keep
enum class BackUpSeedPhraseType {
    /** Back up the words, then verify them by restoring onto a device. */
    INTRO,

    /** The on-chain timelock confirmation that the backup was verified. */
    SUCCESS,

    /**
     * The off-chain inheritance confirmation, which names the key whose backup was proven. It is
     * a separate screen rather than a variant of [SUCCESS]: the two say different things and the
     * caller always knows which flow it is in.
     */
    INHERITANCE_VERIFIED,
}