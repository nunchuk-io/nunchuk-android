#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>
#include "serializer.h"
#include "provider.h"

using namespace nunchuk;

WalletType Serializer::convert2CWalletType(jint ordinal) {
    syslog(LOG_DEBUG, "[JNI][Serializer::convert2CWalletType]ordinal:: %d", ordinal);
    WalletType type;
    switch (ordinal) {
        case 0:
            type = WalletType::SINGLE_SIG;
            break;
        case 1:
            type = WalletType::MULTI_SIG;
            break;
        default:
            type = WalletType::ESCROW;
            break;
    }
    return type;
}

AddressType Serializer::convert2CAddressType(jint ordinal) {
    syslog(LOG_DEBUG, "[JNI][Serializer::convert2CAddressType]ordinal:: %d", ordinal);
    AddressType type;
    switch (ordinal) {
        case 0:
            type = AddressType::ANY;
            break;
        case 1:
            type = AddressType::LEGACY;
            break;
        case 2:
            type = AddressType::NESTED_SEGWIT;
            break;
        default:
            type = AddressType::NATIVE_SEGWIT;
            break;
    }
    syslog(LOG_DEBUG, "[JNI][Serializer::convert2CAddressType]ordinal:: %d", type);
    return type;
}

Chain Serializer::convert2CChain(jint ordinal) {
    syslog(LOG_DEBUG, "[JNI][Serializer::convert2CChain]ordinal:: %d", ordinal);
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

BackendType Serializer::convert2CBackendType(jint ordinal) {
    syslog(LOG_DEBUG, "[JNI][Serializer::convert2CBackendType]ordinal:: %d", ordinal);
    return ordinal == 0 ? BackendType::ELECTRUM : BackendType::CORERPC;
}

SingleSigner Serializer::convert2CSigner(JNIEnv *env, jobject signer) {
    jclass clazz = env->FindClass("com/nunchuk/android/model/SingleSigner");

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

std::vector<SingleSigner> Serializer::convert2CSigners(JNIEnv *env, jobject signers) {
    jclass cList = env->FindClass("java/util/List");

    jmethodID sizeMethod = env->GetMethodID(cList, "size", "()I");
    jmethodID getMethod = env->GetMethodID(cList, "get", "(I)Ljava/lang/Object;");

    jint size = env->CallIntMethod(signers, sizeMethod);
    std::vector<SingleSigner> result;
    for (jint i = 0; i < size; i++) {
        auto item = (jobject) env->CallObjectMethod(signers, getMethod, i);
        const SingleSigner singleSigner = Serializer::convert2CSigner(env, item);
        result.push_back(singleSigner);
    }
    return result;
}