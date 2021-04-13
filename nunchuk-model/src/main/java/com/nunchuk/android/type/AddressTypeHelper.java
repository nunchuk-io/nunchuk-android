package com.nunchuk.android.type;

// This class should be kept in java since jni can't access object class
public final class AddressTypeHelper {

    private AddressTypeHelper() {
    }

    public static AddressType from(final int ordinal) {
        for (final AddressType value : AddressType.values()) {
            if (value.ordinal() == ordinal) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid ordinal " + ordinal);
    }

}