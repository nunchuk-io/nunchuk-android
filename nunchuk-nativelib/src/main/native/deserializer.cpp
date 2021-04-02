#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>
#include "deserializer.h"

using namespace nunchuk;

jobject Deserializer::convert2JSigner(JNIEnv *env, const SingleSigner &signer) {
    jclass clazz = env->FindClass("com/nunchuk/android/model/SingleSigner");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setName", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_name().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setXpub", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_xpub().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setDerivationPath", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_derivation_path().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterFingerprint", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_master_fingerprint().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setPublicKey", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_public_key().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterSignerId", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_master_signer_id().c_str()));
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setLastHealthCheck", "(J)V"), signer.get_last_health_check());
    env->CallVoidMethod(instance, env->GetMethodID(clazz, "setUsed", "(Z)V"), signer.is_used());
    return instance;
}

jobject Deserializer::convert2JSigners(JNIEnv *env, const std::vector<SingleSigner> &signers, jobject result) {
    syslog(LOG_DEBUG, "[JNI] translateSingleSigners()");
    static auto java_util_ArrayList = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    static jmethodID java_util_ArrayList_ = env->GetMethodID(java_util_ArrayList, "<init>", "(I)V");
    jmethodID java_util_ArrayList_add = env->GetMethodID(java_util_ArrayList, "add", "(Ljava/lang/Object;)Z");
    syslog(LOG_DEBUG, "[JNI] signers length %lu", signers.size());
    for (const SingleSigner &s: signers) {
        jobject element = convert2JSigner(env, s);
        env->CallBooleanMethod(result, java_util_ArrayList_add, element);
        env->DeleteLocalRef(element);
    }
    return result;
}