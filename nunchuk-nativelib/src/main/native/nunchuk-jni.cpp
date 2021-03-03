#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

std::unique_ptr<Nunchuk> getOrCreateNunchuk();

jobject translate(const SingleSigner &signer);

jobject translate(JNIEnv *pEnv, const SingleSigner &signer);

std::unique_ptr<Nunchuk> getOrCreateNunchuk() {
    AppSettings settings;
    settings.set_chain(Chain::TESTNET);
    settings.set_hwi_path("bin/hwi");
    settings.enable_proxy(false);
    settings.set_testnet_servers({"testnet.nunchuk.io:50001"});
    settings.set_backend_type(BackendType::ELECTRUM);
    settings.set_storage_path("/data/user/0/com.nunchuk.android/files/nunchuk");
    auto nunchuk = MakeNunchuk(settings);
    return nunchuk;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_createSigner(JNIEnv *env, jobject thiz, jstring name, jstring xpub, jstring public_key, jstring derivation_path, jstring master_fingerprint) {
        const SingleSigner &signer = getOrCreateNunchuk()->CreateSigner(
                env->GetStringUTFChars(name, nullptr),
                env->GetStringUTFChars(xpub, nullptr),
                env->GetStringUTFChars(public_key, nullptr),
                env->GetStringUTFChars(derivation_path, nullptr),
                env->GetStringUTFChars(master_fingerprint, nullptr)
        );
        return translate(env, signer);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getFirstRemoteSigner(JNIEnv *env, jobject thiz) {
    const std::vector<SingleSigner> &vector = getOrCreateNunchuk()->GetRemoteSigners();
    if (vector.empty()) {
        syslog(LOG_DEBUG, "There is no signer");
        return nullptr;
    } else {
        syslog(LOG_DEBUG, "There are existing signers:: %lu", vector.size());
        SingleSigner signer = *(vector.begin());
        return translate(env, signer);
    }
}

jobject translate(JNIEnv *env, const SingleSigner &signer) {
    jclass clazz = env->FindClass("com/nunchuk/android/model/SingleSigner");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    if (instance == 0) syslog(LOG_DEBUG, "NULL RETURNED");

    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setName", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_name().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setXpub", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_xpub().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setDerivationPath", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_derivation_path().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterFingerprint", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_master_fingerprint().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterSignerId", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_master_signer_id().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setLastHealthCheck", "(J)V"), signer.get_last_health_check());
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setUsed", "(Z)V"), signer.is_used());
    syslog(LOG_DEBUG, "name:: %s", signer.get_name().c_str());
    syslog(LOG_DEBUG, "masterFingerprint:: %s", signer.get_master_fingerprint().c_str());
    return instance;
}
