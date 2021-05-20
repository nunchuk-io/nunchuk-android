#include <cstring>
#include <jni.h>
#include <syslog.h>
#include <nunchuk.h>
#include "deserializer.h"

using namespace nunchuk;

jobject Deserializer::convert2JBoolean(JNIEnv *env, const bool value) {
    jclass clazz = env->FindClass("java/lang/Boolean");
    jmethodID methodId = env->GetMethodID(clazz, "<init>", "(Z)V");
    return env->NewObject(clazz, methodId, value ? JNI_TRUE : JNI_FALSE);
}

jobject Deserializer::convert2JListString(JNIEnv *env, const std::vector<std::string> &values) {
    static auto arrayListClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    static jmethodID constructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject arrayListInstance = env->NewObject(arrayListClass, constructor);
    for (const std::string &s: values) {
        auto element = env->NewStringUTF(s.c_str());
        env->CallBooleanMethod(arrayListInstance, addMethod, element);
        env->DeleteLocalRef(element);
    }
    return arrayListInstance;
}

jobject Deserializer::convert2JSignersMap(JNIEnv *env, const std::map<std::string, bool> signersMap) {
    jclass clazz = env->FindClass("java/util/HashMap");
    jmethodID init = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, init);
    try {
        jmethodID putMethod = env->GetMethodID(clazz, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        if (!signersMap.empty()) {
            for (const auto &it : signersMap) {
                env->CallObjectMethod(instance, putMethod, env->NewStringUTF(it.first.c_str()), convert2JBoolean(env, it.second));
            }
        }
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JSignersMap error::%s", e.what());
    }
    return instance;
}

void Deserializer::convert2JException(JNIEnv *env, const char *msg) {
    jclass clazz = env->FindClass("com/nunchuk/android/exception/NCNativeException");
    if (nullptr == clazz) {
        clazz = env->FindClass("java/lang/NullPointerException");
    }
    env->ThrowNew(clazz, msg);
}

jobject Deserializer::convert2JDevice(JNIEnv *env, const Device &device) {
    syslog(LOG_DEBUG, "[JNI] convert2JDevice()");
    jclass clazz = env->FindClass("com/nunchuk/android/model/Device");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    try {
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterFingerprint", "(Ljava/lang/String;)V"), env->NewStringUTF(device.get_master_fingerprint().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setType", "(Ljava/lang/String;)V"), env->NewStringUTF(device.get_type().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setModel", "(Ljava/lang/String;)V"), env->NewStringUTF(device.get_model().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setPath", "(Ljava/lang/String;)V"), env->NewStringUTF(device.get_path().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setConnected", "(Z)V"), device.connected());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setNeedPassPhraseSent", "(Z)V"), device.needs_pass_phrase_sent());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setNeedPinSet", "(Z)V"), device.needs_pin_sent());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setInitialized", "(Z)V"), device.initialized());
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JDevice error::%s", e.what());
    }
    return instance;
}

jobject Deserializer::convert2JAmount(JNIEnv *env, const Amount amount) {
    syslog(LOG_DEBUG, "[JNI] convert2JAmount()");
    jclass clazz = env->FindClass("com/nunchuk/android/model/Amount");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    try {
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setValue", "(J)V"), (long) amount);
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setFormattedValue", "(Ljava/lang/String;)V"), env->NewStringUTF(Utils::ValueFromAmount(amount).c_str()));
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JAmount error::%s", e.what());
    }
    return instance;
}

jobject Deserializer::convert2JTxInput(JNIEnv *env, const TxInput input) {
    syslog(LOG_DEBUG, "[JNI] convert2JTxInput()");
    jclass clazz = env->FindClass("com/nunchuk/android/model/TxInput");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    try {
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setFirst", "(Ljava/lang/String;)V"), env->NewStringUTF(input.first.c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setSecond", "(I)V"), input.second);
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JTxInput error::%s", e.what());
    }
    return instance;
}

jobject Deserializer::convert2JTxInputs(JNIEnv *env, const std::vector<TxInput> inputs) {
    static auto arrayListClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    static jmethodID constructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject arrayListInstance = env->NewObject(arrayListClass, constructor);
    for (const TxInput &input: inputs) {
        jobject element = convert2JTxInput(env, input);
        env->CallBooleanMethod(arrayListInstance, addMethod, element);
        env->DeleteLocalRef(element);
    }
    return arrayListInstance;
}

jobject Deserializer::convert2JTxOutput(JNIEnv *env, const TxOutput output) {
    syslog(LOG_DEBUG, "[JNI] convert2JTxOutput()");
    jclass clazz = env->FindClass("com/nunchuk/android/model/TxOutput");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    try {
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setFirst", "(Ljava/lang/String;)V"), env->NewStringUTF(output.first.c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setSecond", "(Lcom/nunchuk/android/model/Amount;)V"), convert2JAmount(env, output.second));
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JTxOutput error::%s", e.what());
    }
    return instance;
}

jobject Deserializer::convert2JTxOutputs(JNIEnv *env, const std::vector<TxOutput> outputs) {
    static auto arrayListClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    static jmethodID constructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject arrayListInstance = env->NewObject(arrayListClass, constructor);
    for (const TxOutput &output: outputs) {
        jobject element = convert2JTxOutput(env, output);
        env->CallBooleanMethod(arrayListInstance, addMethod, element);
        env->DeleteLocalRef(element);
    }
    return arrayListInstance;
}

jobject Deserializer::convert2JAddressType(JNIEnv *env, const AddressType &type) {
    jclass clazz = env->FindClass("com/nunchuk/android/type/AddressTypeHelper");
    jmethodID staticMethod = env->GetStaticMethodID(clazz, "from", "(I)Lcom/nunchuk/android/type/AddressType;");
    return env->CallStaticObjectMethod(clazz, staticMethod, (int) type);
}

jobject Deserializer::convert2JTransactionStatus(JNIEnv *env, const TransactionStatus &status) {
    jclass clazz = env->FindClass("com/nunchuk/android/type/TransactionStatusHelper");
    jmethodID staticMethod = env->GetStaticMethodID(clazz, "from", "(I)Lcom/nunchuk/android/type/TransactionStatus;");
    return env->CallStaticObjectMethod(clazz, staticMethod, (int) status);
}

jobject Deserializer::convert2JMasterSigner(JNIEnv *env, const MasterSigner &signer) {
    syslog(LOG_DEBUG, "[JNI] convert2JMasterSigner()");
    jclass clazz = env->FindClass("com/nunchuk/android/model/MasterSigner");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    try {
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setId", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_id().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setName", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_name().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setDevice", "(Lcom/nunchuk/android/model/Device;)V"), convert2JDevice(env, signer.get_device()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setLastHealthCheck", "(J)V"), signer.get_last_health_check());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setSoftware", "(Z)V"), signer.is_software());
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JSigner error::%s", e.what());
    }
    return instance;
}

jobject Deserializer::convert2JSigner(JNIEnv *env, const SingleSigner &signer) {
    syslog(LOG_DEBUG, "[JNI] convert2JSigner()");
    jclass clazz = env->FindClass("com/nunchuk/android/model/SingleSigner");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    try {
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setName", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_name().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setXpub", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_xpub().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setDerivationPath", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_derivation_path().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterFingerprint", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_master_fingerprint().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setPublicKey", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_public_key().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMasterSignerId", "(Ljava/lang/String;)V"), env->NewStringUTF(signer.get_master_signer_id().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setLastHealthCheck", "(J)V"), signer.get_last_health_check());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setUsed", "(Z)V"), signer.is_used());
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JSigner error::%s", e.what());
    }
    return instance;
}

jobject Deserializer::convert2JSigners(JNIEnv *env, const std::vector<SingleSigner> &signers) {
    syslog(LOG_DEBUG, "[JNI] convert2JSigners()");
    static auto arrayListClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    static jmethodID constructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject arrayListInstance = env->NewObject(arrayListClass, constructor);
    for (const SingleSigner &s: signers) {
        jobject element = convert2JSigner(env, s);
        env->CallBooleanMethod(arrayListInstance, addMethod, element);
        env->DeleteLocalRef(element);
    }
    return arrayListInstance;
}

jobject Deserializer::convert2JMasterSigners(JNIEnv *env, const std::vector<MasterSigner> &signers) {
    syslog(LOG_DEBUG, "[JNI] convert2JMasterSigners()");
    static auto arrayListClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    static jmethodID constructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject arrayListInstance = env->NewObject(arrayListClass, constructor);
    for (const MasterSigner &s: signers) {
        jobject element = convert2JMasterSigner(env, s);
        env->CallBooleanMethod(arrayListInstance, addMethod, element);
        env->DeleteLocalRef(element);
    }
    return arrayListInstance;
}

jobject Deserializer::convert2JWallet(JNIEnv *env, const Wallet &wallet) {
    jclass clazz = env->FindClass("com/nunchuk/android/model/Wallet");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    try {
        jobject signers = convert2JSigners(env, wallet.get_signers());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setId", "(Ljava/lang/String;)V"), env->NewStringUTF(wallet.get_id().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setName", "(Ljava/lang/String;)V"), env->NewStringUTF(wallet.get_name().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setTotalRequireSigns", "(I)V"), wallet.get_n());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setSigners", "(Ljava/util/List;)V"), signers);
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setEscrow", "(Z)V"), wallet.is_escrow());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setBalance", "(Lcom/nunchuk/android/model/Amount;)V"), convert2JAmount(env, wallet.get_balance()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setAddressType", "(Lcom/nunchuk/android/type/AddressType;)V"), convert2JAddressType(env, wallet.get_address_type()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setCreateDate", "(J)V"), wallet.get_create_date());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setDescription", "(Ljava/lang/String;)V"), env->NewStringUTF(wallet.get_description().c_str()));
        syslog(LOG_DEBUG, "[JNI] convert2JWallet balance::%s", Utils::ValueFromAmount(wallet.get_balance()).c_str());
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JWallet error::%s", e.what());
        convert2JException(env, e.what());
    }
    return instance;
}

jobject Deserializer::convert2JWallets(JNIEnv *env, const std::vector<Wallet> &wallets) {
    syslog(LOG_DEBUG, "[JNI] convert2JWallets()");
    static auto arrayListClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    static jmethodID constructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jobject arrayListInstance = env->NewObject(arrayListClass, constructor);
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    if (wallets.empty()) {
        syslog(LOG_DEBUG, "[JNI] wallets empty");
        return arrayListInstance;
    } else {
        syslog(LOG_DEBUG, "[JNI] wallets size:: %lu", wallets.size());
        for (const Wallet &s: wallets) {
            jobject element = convert2JWallet(env, s);
            env->CallBooleanMethod(arrayListInstance, addMethod, element);
            env->DeleteLocalRef(element);
        }
    }
    return arrayListInstance;
}

jobject Deserializer::convert2JTransaction(JNIEnv *env, const Transaction &transaction) {
    syslog(LOG_DEBUG, "[JNI] convert2JTransaction()");
    jclass clazz = env->FindClass("com/nunchuk/android/model/Transaction");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject instance = env->NewObject(clazz, constructor);
    try {
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setTxId", "(Ljava/lang/String;)V"), env->NewStringUTF(transaction.get_txid().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setInputs", "(Ljava/util/List;)V"), convert2JTxInputs(env, transaction.get_inputs()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setOutputs", "(Ljava/util/List;)V"), convert2JTxOutputs(env, transaction.get_outputs()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setReceiveOutput", "(Ljava/util/List;)V"), convert2JTxOutputs(env, transaction.get_receive_outputs()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setChangeIndex", "(I)V"), transaction.get_change_index());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setM", "(I)V"), transaction.get_m());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setSigners", "(Ljava/util/Map;)V"), convert2JSignersMap(env, transaction.get_signers()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setMemo", "(Ljava/lang/String;)V"), env->NewStringUTF(transaction.get_memo().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setStatus", "(Lcom/nunchuk/android/type/TransactionStatus;)V"), convert2JTransactionStatus(env, transaction.get_status()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setReplacedByTxid", "(Ljava/lang/String;)V"), env->NewStringUTF(transaction.get_replaced_by_txid().c_str()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setFee", "(Lcom/nunchuk/android/model/Amount;)V"), convert2JAmount(env, transaction.get_fee()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setFeeRate", "(Lcom/nunchuk/android/model/Amount;)V"), convert2JAmount(env, transaction.get_fee_rate()));
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setBlockTime", "(J)V"), transaction.get_blocktime());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setSubtractFeeFromAmount", "(Z)V"), transaction.subtract_fee_from_amount());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setReceive", "(Z)V"), transaction.is_receive());
        env->CallVoidMethod(instance, env->GetMethodID(clazz, "setSubAmount", "(Lcom/nunchuk/android/model/Amount;)V"), convert2JAmount(env, transaction.get_sub_amount()));
    } catch (const std::exception &e) {
        syslog(LOG_DEBUG, "[JNI] convert2JTransaction error::%s", e.what());
    }
    return instance;
}

jobject Deserializer::convert2JTransactions(JNIEnv *env, const std::vector<Transaction> &transactions) {
    syslog(LOG_DEBUG, "[JNI] convert2JMasterSigners()");
    static auto arrayListClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    static jmethodID constructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject arrayListInstance = env->NewObject(arrayListClass, constructor);
    for (const Transaction &s: transactions) {
        jobject element = convert2JTransaction(env, s);
        env->CallBooleanMethod(arrayListInstance, addMethod, element);
        env->DeleteLocalRef(element);
    }
    return arrayListInstance;
}
