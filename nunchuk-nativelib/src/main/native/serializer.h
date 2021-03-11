#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

/**
 * Used to translate Java properties into C++ equivalents
 */
class Serializer {

public:
    static Chain intToChain(jint ordinal);

    static BackendType intToBackendType(jint ordinal);
};