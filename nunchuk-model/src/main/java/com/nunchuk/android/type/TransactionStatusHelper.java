package com.nunchuk.android.type;

// This class should be kept in java since jni can't access object class
public final class TransactionStatusHelper {

    private TransactionStatusHelper() {
    }

    public static TransactionStatus from(final int ordinal) {
        for (final TransactionStatus value : TransactionStatus.values()) {
            if (value.ordinal() == ordinal) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid ordinal " + ordinal);
    }

}