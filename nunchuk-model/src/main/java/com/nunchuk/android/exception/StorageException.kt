package com.nunchuk.android.exception

class StorageException(override val code: Int) : BaseException(code) {

    companion object {
        const val WALLET_NOT_FOUND = -2001
        const val MASTERSIGNER_NOT_FOUND = -2002
        const val TX_NOT_FOUND = -2003
        const val SIGNER_USED = -2005
        const val INVALID_DATADIR = -2006
        const val SQL_ERROR = -2007
        const val WALLET_EXISTED = -2008
        const val SIGNER_EXISTS = -2009
        const val SIGNER_NOT_FOUND = -2010
        const val ADDRESS_NOT_FOUND = -2011
    }

}