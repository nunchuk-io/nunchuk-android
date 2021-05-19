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
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_broadcastTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring tx_id
) {
    syslog(LOG_DEBUG, "[JNI]broadcastTransaction()");
    auto transaction = NunchukProvider::get()->nu->BroadcastTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(tx_id, JNI_FALSE)
    );
    return Deserializer::convert2JTransaction(env, transaction);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_createTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jobject outputs,
        jstring memo,
        jobject inputs,
        jobject fee_rate,
        jboolean subtract_fee_from_amount
) {
    syslog(LOG_DEBUG, "[JNI]createTransaction()");
    auto transaction = NunchukProvider::get()->nu->CreateTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            Serializer::convert2CAmountsMap(env, outputs),
            env->GetStringUTFChars(memo, JNI_FALSE),
            Serializer::convert2CUnspentOutputs(env, inputs),
            Serializer::convert2CAmount(env, fee_rate),
            subtract_fee_from_amount
    );
    return Deserializer::convert2JTransaction(env, transaction);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_draftTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jobject outputs,
        jobject inputs,
        jobject fee_rate,
        jboolean subtract_fee_from_amount
) {
    syslog(LOG_DEBUG, "[JNI]draftTransaction()");
    auto transaction = NunchukProvider::get()->nu->DraftTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            Serializer::convert2CAmountsMap(env, outputs),
            Serializer::convert2CUnspentOutputs(env, inputs),
            Serializer::convert2CAmount(env, fee_rate),
            subtract_fee_from_amount
    );
    return Deserializer::convert2JTransaction(env, transaction);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_deleteTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring tx_id
) {
    return NunchukProvider::get()->nu->DeleteTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(tx_id, JNI_FALSE)
    );
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_exportCoboTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring tx_id
) {
    auto values = NunchukProvider::get()->nu->ExportCoboTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(tx_id, JNI_FALSE)
    );
    return Deserializer::convert2JListString(env, values);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_exportTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring tx_id,
        jstring file_path
) {
    return NunchukProvider::get()->nu->ExportTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(tx_id, JNI_FALSE),
            env->GetStringUTFChars(file_path, JNI_FALSE)
    );
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring tx_id
) {
    auto transaction = NunchukProvider::get()->nu->GetTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(tx_id, JNI_FALSE)
    );
    return Deserializer::convert2JTransaction(env, transaction);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_replaceTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring tx_id,
        jobject new_fee_rate
) {
    auto transaction = NunchukProvider::get()->nu->ReplaceTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(tx_id, JNI_FALSE),
            Serializer::convert2CAmount(env, new_fee_rate)
    );
    return Deserializer::convert2JTransaction(env, transaction);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_importTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring file_path
) {
    auto transaction = NunchukProvider::get()->nu->ImportTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(file_path, JNI_FALSE)
    );
    return Deserializer::convert2JTransaction(env, transaction);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getTransactionHistory(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jint count,
        jint skip
) {
    auto transactions = NunchukProvider::get()->nu->GetTransactionHistory(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            count,
            skip
    );
    return Deserializer::convert2JTransactions(env, transactions);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_importCoboTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jobject qr_data
) {
    // TODO: implement importCoboTransaction()
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_updateTransactionMemo(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring tx_id,
        jstring new_memo
) {
    return NunchukProvider::get()->nu->UpdateTransactionMemo(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(tx_id, JNI_FALSE),
            env->GetStringUTFChars(new_memo, JNI_FALSE)
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_signTransaction(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring tx_id,
        jobject device
) {
    NunchukProvider::get()->nu->SignTransaction(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(tx_id, JNI_FALSE),
            Serializer::convert2CDevice(env, device)
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_exportTransactionHistory(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring file_path,
        jint format
) {
    NunchukProvider::get()->nu->ExportTransactionHistory(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(file_path, JNI_FALSE),
            Serializer::convert2CExportFormat(format)
    );
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getAddresses(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jboolean used,
        jboolean internal
) {
    auto values = NunchukProvider::get()->nu->GetAddresses(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            used,
            internal);
    return Deserializer::convert2JListString(env, values);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getAddressBalance(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jstring address
) {
    auto value = NunchukProvider::get()->nu->GetAddressBalance(
            env->GetStringUTFChars(wallet_id, JNI_FALSE),
            env->GetStringUTFChars(address, JNI_FALSE));
    return Deserializer::convert2JAmount(env, value);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getUnspentOutputs(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id
) {
    // TODO: implement getUnspentOutputs()
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_newAddress(
        JNIEnv *env,
        jobject thiz,
        jstring wallet_id,
        jboolean internal
) {
    try {
        auto value = NunchukProvider::get()->nu->NewAddress(
                env->GetStringUTFChars(wallet_id, JNI_FALSE),
                internal);
        return env->NewStringUTF(value.c_str());
    } catch (std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] newAddress error::%s", e.what());
        Deserializer::convert2JException(env, e.what());
        env->ExceptionOccurred();
        return env->NewStringUTF("");
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_valueFromAmount(
        JNIEnv *env,
        jobject thiz,
        jobject amount
) {
    try {
        Amount _amount = Serializer::convert2CAmount(env, amount);
        auto value = Utils::ValueFromAmount(_amount);
        return env->NewStringUTF(value.c_str());
    } catch (std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] valueFromAmount error::%s", e.what());
        Deserializer::convert2JException(env, e.what());
        env->ExceptionOccurred();
        return env->NewStringUTF("");
    }
}