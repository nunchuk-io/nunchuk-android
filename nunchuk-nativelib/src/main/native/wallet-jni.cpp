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
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getWallets(JNIEnv *env, jobject thiz) {
    syslog(LOG_DEBUG, "[JNI]getWallets()");
    auto wallets = NunchukProvider::get()->nu->GetWallets();
    syslog(LOG_DEBUG, "[JNI]wallets::%lu", wallets.size());
    return Deserializer::convert2JWallets(env, wallets);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_draftWallet(
        JNIEnv *env,
        jobject thiz,
        jstring name,
        jint total_require_signs,
        jobject signers,
        jint address_type,
        jboolean is_escrow,
        jstring description
) {
    try {
        const std::vector<SingleSigner> &singleSigners = Serializer::convert2CSigners(env, signers);
        AddressType type = Serializer::convert2CAddressType(address_type);
        auto filePath = NunchukProvider::get()->nu->DraftWallet(
                env->GetStringUTFChars(name, nullptr),
                singleSigners.size(),
                total_require_signs,
                singleSigners,
                type,
                is_escrow,
                env->GetStringUTFChars(description, nullptr)
        );
        return env->NewStringUTF(filePath.c_str());
    } catch (std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] draftWallet error::%s", e.what());
        env->ExceptionOccurred();
        return env->NewStringUTF("");
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_createWallet(
        JNIEnv *env,
        jobject thiz,
        jstring name,
        jint total_require_signs,
        jobject signers,
        jint address_type,
        jboolean is_escrow,
        jstring description
) {
    try {
        const std::vector<SingleSigner> &singleSigners = Serializer::convert2CSigners(env, signers);
        AddressType type = Serializer::convert2CAddressType(address_type);
        const Wallet &wallet = NunchukProvider::get()->nu->CreateWallet(
                env->GetStringUTFChars(name, nullptr),
                singleSigners.size(),
                total_require_signs,
                singleSigners,
                type,
                is_escrow,
                env->GetStringUTFChars(description, nullptr)
        );
        syslog(LOG_DEBUG, "[JNI][wallet]name::%s", wallet.get_name().c_str());
        syslog(LOG_DEBUG, "[JNI][wallet]address_type::%d", wallet.get_address_type());
        syslog(LOG_DEBUG, "[JNI][wallet]signers::%lu", wallet.get_signers().size());
        return Deserializer::convert2JWallet(env, wallet);
    } catch (std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] createWallet error::%s", e.what());
        Deserializer::convert2JException(env, e.what());
        return env->ExceptionOccurred();
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_exportWallet(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring file_path,
        jint format
) {
    try {
        return NunchukProvider::get()->nu->ExportWallet(
                env->GetStringUTFChars(wallet_id, nullptr),
                env->GetStringUTFChars(file_path, nullptr),
                Serializer::convert2CExportFormat(format)
        );
    } catch (std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] createWallet error::%s", e.what());
        Deserializer::convert2JException(env, e.what());
        env->ExceptionOccurred();
        return 0;
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_exportCoboWallet(JNIEnv *env, jobject thiz, jstring wallet_id) {
    try {
        auto values =  NunchukProvider::get()->nu->ExportCoboWallet(
                env->GetStringUTFChars(wallet_id, nullptr)
        );
        return Deserializer::convert2JListString(env, values);
    } catch (std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] createWallet error::%s", e.what());
        Deserializer::convert2JException(env, e.what());
        env->ExceptionOccurred();
        return 0;
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getWallet(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id
) {
    try {
        const auto &wallet = NunchukProvider::get()->nu->GetWallet(env->GetStringUTFChars(wallet_id, nullptr));
        return Deserializer::convert2JWallet(env, wallet);
    } catch (std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] getWallet error::%s", e.what());
        Deserializer::convert2JException(env, e.what());
        return env->ExceptionOccurred();
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_updateWallet(
        JNIEnv *env,
        jobject thiz,
        jobject wallet
) {
    auto updateWallet = Serializer::convert2CWallet(env, wallet);
    return NunchukProvider::get()->nu->UpdateWallet(updateWallet);
}
