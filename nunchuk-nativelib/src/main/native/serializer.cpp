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

SingleSigner Serializer::convert2CSigner(JNIEnv *env, jobject signer) {
    jclass clazz = env->GetObjectClass(signer);
    //jclass clazz = env->FindClass("com/nunchuk/android/model/SingleSigner");

    jfieldID fieldName = env->GetFieldID(clazz, "name", "Ljava/lang/String;");
    auto nameVal = (jstring) env->GetObjectField(signer, fieldName);
    const char *name = env->GetStringUTFChars(nameVal, nullptr);

    jfieldID fieldXpub = env->GetFieldID(clazz, "xpub", "Ljava/lang/String;");
    auto xpubVal = (jstring) env->GetObjectField(signer, fieldXpub);
    const char *xpub = env->GetStringUTFChars(xpubVal, nullptr);

    jfieldID fieldPublicKey = env->GetFieldID(clazz, "publicKey", "Ljava/lang/String;");
    auto publicKeyVal = (jstring) env->GetObjectField(signer, fieldPublicKey);
    const char *public_key = env->GetStringUTFChars(publicKeyVal, nullptr);

    jfieldID fieldDerivationPath = env->GetFieldID(clazz, "derivationPath", "Ljava/lang/String;");
    auto derivationPathVal = (jstring) env->GetObjectField(signer, fieldDerivationPath);
    const char *derivation_path = env->GetStringUTFChars(derivationPathVal, nullptr);

    jfieldID fieldMasterFingerprint = env->GetFieldID(clazz, "masterFingerprint", "Ljava/lang/String;");
    auto masterFingerprintVal = (jstring) env->GetObjectField(signer, fieldMasterFingerprint);
    const char *master_fingerprint = env->GetStringUTFChars(masterFingerprintVal, nullptr);

    const SingleSigner &singleSigner = SingleSigner(name, xpub, public_key, derivation_path, master_fingerprint, 0);
    syslog(LOG_DEBUG, "[JNI][SingleSigner]name:: %s", singleSigner.get_name().c_str());
    syslog(LOG_DEBUG, "[JNI][SingleSigner]xpub:: %s", singleSigner.get_xpub().c_str());
    syslog(LOG_DEBUG, "[JNI][SingleSigner]path:: %s", singleSigner.get_derivation_path().c_str());
    syslog(LOG_DEBUG, "[JNI][SingleSigner]fingerPrint:: %s", singleSigner.get_master_fingerprint().c_str());

    env->ReleaseStringUTFChars(nameVal, name);
    env->ReleaseStringUTFChars(xpubVal, xpub);
    env->ReleaseStringUTFChars(publicKeyVal, public_key);
    env->ReleaseStringUTFChars(derivationPathVal, derivation_path);
    env->ReleaseStringUTFChars(masterFingerprintVal, master_fingerprint);
    return singleSigner;
}