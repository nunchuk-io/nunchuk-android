#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>
#include "serializer.h"
#include "provider.h"

using namespace nunchuk;

Chain Serializer::intToChain(jint ordinal) {
    Chain chain;
    switch (ordinal) {
        case 0:
            chain = Chain::MAIN;
            break;
        case 1:
            chain = Chain::TESTNET;
            break;
        default:
            chain = Chain::REGTEST;
            break;
    }
    return chain;
}

BackendType Serializer::intToBackendType(jint ordinal) {
    return ordinal == 0 ? BackendType::ELECTRUM : BackendType::CORERPC;
}
