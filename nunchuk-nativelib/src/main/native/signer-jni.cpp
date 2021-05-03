#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>
#include "provider.h"
#include "serializer.h"
#include "deserializer.h"

using namespace nunchuk;

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_createSigner(
        JNIEnv *env,
        jobject thiz,
        jstring name,
        jstring xpub,
        jstring public_key,
        jstring derivation_path,
        jstring master_fingerprint
) {
    syslog(LOG_DEBUG, "[JNI]createSigner()");
    try {
        const SingleSigner &signer = NunchukProvider::get()->nu->CreateSigner(
                env->GetStringUTFChars(name, nullptr),
                env->GetStringUTFChars(xpub, nullptr),
                env->GetStringUTFChars(public_key, nullptr),
                env->GetStringUTFChars(derivation_path, nullptr),
                env->GetStringUTFChars(master_fingerprint, nullptr)
        );
        syslog(LOG_DEBUG, "[JNI][signer]name::%s", signer.get_name().c_str());
        syslog(LOG_DEBUG, "[JNI][signer]public_key::%s", signer.get_public_key().c_str());
        syslog(LOG_DEBUG, "[JNI][signer]xpub::%s", signer.get_xpub().c_str());
        return Deserializer::convert2JSigner(env, signer);
    } catch (std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] createSigner error::%s", e.what());
        Deserializer::convert2JException(env, e.what());
        return env->ExceptionOccurred();
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getRemoteSigner(
        JNIEnv *env,
        jobject thiz
) {
    syslog(LOG_DEBUG, "[JNI]getRemoteSigner()");
    auto signers = NunchukProvider::get()->nu->GetRemoteSigners();
    syslog(LOG_DEBUG, "[JNI]nu->GetRemoteSigners()");
    if (signers.empty()) {
        syslog(LOG_DEBUG, "[JNI]There is no signer");
        return nullptr;
    } else {
        syslog(LOG_DEBUG, "There is existing signers:: %lu", signers.size());
        SingleSigner signer = *(signers.begin());
        return Deserializer::convert2JSigner(env, signer);
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getRemoteSigners(JNIEnv *env, jobject thiz) {
    syslog(LOG_DEBUG, "[JNI]getRemoteSigner()");
    auto signers = NunchukProvider::get()->nu->GetRemoteSigners();
    return Deserializer::convert2JSigners(env, signers);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_deleteRemoteSigner(
        JNIEnv *env,
        jobject thiz,
        jstring master_fingerprint,
        jstring derivation_path
) {
    NunchukProvider::get()->nu->DeleteRemoteSigner(
            env->GetStringUTFChars(master_fingerprint, nullptr),
            env->GetStringUTFChars(derivation_path, nullptr
            ));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_updateSigner(
        JNIEnv *env,
        jobject thiz,
        jobject signer
) {
    auto singleSigner = Serializer::convert2CSigner(env, signer);
    NunchukProvider::get()->nu->UpdateRemoteSigner(singleSigner);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_generateMnemonic(
        JNIEnv *env,
        jobject thiz
) {
    return env->NewStringUTF(Utils::GenerateMnemonic().c_str());
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getBip39WordList(
        JNIEnv *env,
        jobject thiz
) {
    return Deserializer::convert2JListString(env, Utils::GetBip39WordList());
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_checkMnemonic(
        JNIEnv *env,
        jobject thiz,
        jstring mnemonic
) {
    return Utils::CheckMnemonic(env->GetStringUTFChars(mnemonic, nullptr));
}