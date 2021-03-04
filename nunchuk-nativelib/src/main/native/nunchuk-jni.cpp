#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>

using namespace nunchuk;

class NunchukProvider {
    static NunchukProvider *_instance;

    NunchukProvider() {
        nu = getOrCreateNunchuk();
    }

public:
    static std::unique_ptr<Nunchuk> getOrCreateNunchuk() {
        AppSettings settings;
        settings.set_chain(Chain::TESTNET);
        settings.set_hwi_path("bin/hwi");
        settings.enable_proxy(false);
        settings.set_testnet_servers({"testnet.nunchuk.io:50001"});
        settings.set_backend_type(BackendType::ELECTRUM);
        settings.set_storage_path("/storage/emulated/0/nunchuk");
        return MakeNunchuk(settings);
    }

    static NunchukProvider *get() {
        if (!_instance) {
            _instance = new NunchukProvider;
        }
        return _instance;
    }

    std::unique_ptr<Nunchuk> nu;
};

jobject translate(JNIEnv *pEnv, SingleSigner signer);

void newwallet();

NunchukProvider *NunchukProvider::_instance = nullptr;

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_createSigner(JNIEnv *env, jobject thiz, jstring name, jstring xpub, jstring public_key, jstring derivation_path, jstring master_fingerprint) {
    try {
        const SingleSigner &signer = NunchukProvider::get()->nu->CreateSigner(
                env->GetStringUTFChars(name, nullptr),
                env->GetStringUTFChars(xpub, nullptr),
                env->GetStringUTFChars(public_key, nullptr),
                env->GetStringUTFChars(derivation_path, nullptr),
                env->GetStringUTFChars(master_fingerprint, nullptr)
        );
        return translate(env, signer);
    }
    catch (StorageException &exception) {
        syslog(LOG_CRIT, "[JNI]create signer error %s", exception.what());
        return nullptr;
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nunchuk_android_nativelib_LibNunchukAndroid_getRemoteSigner(JNIEnv *env, jobject thiz) {
    syslog(LOG_DEBUG, "[JNI]getRemoteSigner()");
    newwallet();
    syslog(LOG_DEBUG, "[JNI]newwallet()");
    auto signers = NunchukProvider::get()->nu->GetRemoteSigners();
    syslog(LOG_DEBUG, "[JNI]nu->GetRemoteSigners()");
    if (signers.empty()) {
        syslog(LOG_DEBUG, "[JNI]There is no signer");
        return nullptr;
    } else {
        syslog(LOG_DEBUG, "There is existing signers:: %lu", signers.size());
        SingleSigner signer = *(signers.begin());
        return translate(env, signer);
    }
}

void newwallet() {
    try {
        auto signers = NunchukProvider::get()->nu->GetRemoteSigners();
        if (signers.empty()) {
            throw std::runtime_error("[JNI]Please create signer first");
        }

        auto name = "ECoinWallet";
        AddressType address_type = AddressType::NATIVE_SEGWIT;
        auto wallet = NunchukProvider::get()->nu->CreateWallet(name, signers.size(), signers.size(), signers, address_type, false);
        syslog(LOG_DEBUG, "[JNI]Wallet create success. Wallet id: %s", wallet.get_id().c_str());
    } catch (StorageException &exception) {
        syslog(LOG_CRIT, "[JNI]Wallet exists %s", exception.what());

    }
}

jobject translate(JNIEnv *env, SingleSigner signer) {
    jclass clazz = env->FindClass("com/nunchuk/android/model/SingleSigner");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    if (instance == nullptr) syslog(LOG_DEBUG, "[JNI] NULL RETURNED");

    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setName", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_name().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setXpub", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_xpub().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setDerivationPath", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_derivation_path().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterFingerprint", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_master_fingerprint().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterSignerId", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_master_signer_id().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setLastHealthCheck", "(J)V"), signer.get_last_health_check());
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setUsed", "(Z)V"), signer.is_used());
    syslog(LOG_DEBUG, "[JNI]name:: %s", signer.get_name().c_str());
    syslog(LOG_DEBUG, "[JNI]masterFingerprint:: %s", signer.get_master_fingerprint().c_str());
    return instance;
}