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

    static WalletType convert2CWalletType(jint ordinal);

    static AddressType convert2CAddressType(jint ordinal);

    static Chain convert2CChain(jint ordinal);

    static BackendType convert2CBackendType(jint ordinal);

    static SingleSigner convert2CSigner(JNIEnv *env, jobject signer);

    static std::vector<SingleSigner> convert2CSigners(JNIEnv *env, jobject signers);

};