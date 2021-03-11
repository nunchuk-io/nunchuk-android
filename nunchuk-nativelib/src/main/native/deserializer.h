#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

/**
 * Used to convert C++ Classes into Java models
 */
class Deserializer {

public:
    static jobject translateSingleSigner(JNIEnv *pEnv, const SingleSigner &signer);
};