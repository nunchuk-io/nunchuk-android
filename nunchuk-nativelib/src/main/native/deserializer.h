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
    static jobject convert2JSigner(JNIEnv *env, const SingleSigner &signer);

    static jobject convert2JSigners(JNIEnv *env, const std::vector<SingleSigner> &signers, jobject result);
};