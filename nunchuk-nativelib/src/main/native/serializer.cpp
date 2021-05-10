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

ExportFormat Serializer::convert2CExportFormat(jint ordinal) {
    syslog(LOG_DEBUG, "[JNI][Serializer::convert2CExportFormat]ordinal:: %d", ordinal);
    ExportFormat format;
    switch (ordinal) {
        case 0:
            format = ExportFormat::DB;
            break;
        case 1:
            format = ExportFormat::DESCRIPTOR;
            break;
        case 2:
            format = ExportFormat::COLDCARD;
            break;
        case 3:
            format = ExportFormat::COBO;
            break;
        default:
            format = ExportFormat::CSV;
            break;
    }
    return format;
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

MasterSigner Serializer::convert2CMasterSigner(JNIEnv *env, jobject signer) {
    jclass clazz = env->FindClass("com/nunchuk/android/model/MasterSigner");

    jfieldID fieldId = env->GetFieldID(clazz, "id", "Ljava/lang/String;");
    auto idVal = (jstring) env->GetObjectField(signer, fieldId);
    const char *id = env->GetStringUTFChars(idVal, JNI_FALSE);

    jfieldID fieldName = env->GetFieldID(clazz, "name", "Ljava/lang/String;");
    auto nameVal = (jstring) env->GetObjectField(signer, fieldName);
    const char *name = env->GetStringUTFChars(nameVal, JNI_FALSE);

    jfieldID fieldLastHealthCheck = env->GetFieldID(clazz, "lastHealthCheck", "J");
    auto last_health_check =  env->GetLongField(signer, fieldLastHealthCheck);

    jfieldID fieldSoftware = env->GetFieldID(clazz, "software", "Z");
    auto software =  env->GetBooleanField(signer, fieldSoftware);

    jfieldID fieldDevice = env->GetFieldID(clazz, "device", "Lcom/nunchuk/android/model/Device;");
    auto deviceVal = (jobject) env->GetObjectField(signer, fieldDevice);
    auto device = convert2CDevice(env, deviceVal);

    MasterSigner masterSigner = MasterSigner(id, device, last_health_check, software);
    masterSigner.set_name(name);
    syslog(LOG_DEBUG, "[JNI][MasterSigner]id:: %s", masterSigner.get_id().c_str());
    syslog(LOG_DEBUG, "[JNI][MasterSigner]name:: %s", masterSigner.get_name().c_str());
    syslog(LOG_DEBUG, "[JNI][MasterSigner]path:: %s", masterSigner.get_device().get_path().c_str());
    syslog(LOG_DEBUG, "[JNI][MasterSigner]fingerPrint:: %s", masterSigner.get_device().get_master_fingerprint().c_str());

    env->ReleaseStringUTFChars(idVal, id);
    env->ReleaseStringUTFChars(nameVal, name);
    return masterSigner;
}

Device Serializer::convert2CDevice(JNIEnv *env, jobject device) {
    jclass clazz = env->FindClass("com/nunchuk/android/model/Device");

    jfieldID fieldType = env->GetFieldID(clazz, "type", "Ljava/lang/String;");
    auto typeVal = (jstring) env->GetObjectField(device, fieldType);
    const char *type = env->GetStringUTFChars(typeVal, JNI_FALSE);

    jfieldID fieldModel = env->GetFieldID(clazz, "model", "Ljava/lang/String;");
    auto modelVal = (jstring) env->GetObjectField(device, fieldModel);
    const char *model = env->GetStringUTFChars(modelVal, JNI_FALSE);

    jfieldID fieldPath = env->GetFieldID(clazz, "path", "Ljava/lang/String;");
    auto pathVal = (jstring) env->GetObjectField(device, fieldPath);
    const char *path = env->GetStringUTFChars(pathVal, JNI_FALSE);

    jfieldID fieldMasterFingerprint = env->GetFieldID(clazz, "masterFingerprint", "Ljava/lang/String;");
    auto masterFingerprintVal = (jstring) env->GetObjectField(device, fieldMasterFingerprint);
    const char *master_fingerprint = env->GetStringUTFChars(masterFingerprintVal, JNI_FALSE);

    jfieldID fieldConnected = env->GetFieldID(clazz, "connected", "Z");
    auto connected = env->GetBooleanField(device, fieldConnected);

    jfieldID fieldNeedPassPhraseSent = env->GetFieldID(clazz, "needPassPhraseSent", "Z");
    auto needs_pass_phrase_sent = env->GetBooleanField(device, fieldNeedPassPhraseSent);

    jfieldID fieldNeedPinSet = env->GetFieldID(clazz, "needPinSet", "Z");
    auto need_pin_set = env->GetBooleanField(device, fieldNeedPinSet);

    const Device &device_ = Device(type, path, model, master_fingerprint, connected, needs_pass_phrase_sent, need_pin_set);
    syslog(LOG_DEBUG, "[JNI][Device]type:: %s", device_.get_type().c_str());
    syslog(LOG_DEBUG, "[JNI][Device]model:: %s", device_.get_model().c_str());
    syslog(LOG_DEBUG, "[JNI][Device]path:: %s", device_.get_path().c_str());
    syslog(LOG_DEBUG, "[JNI][Device]fingerPrint:: %s", device_.get_master_fingerprint().c_str());

    env->ReleaseStringUTFChars(typeVal, type);
    env->ReleaseStringUTFChars(modelVal, model);
    env->ReleaseStringUTFChars(pathVal, path);
    env->ReleaseStringUTFChars(masterFingerprintVal, master_fingerprint);
    return device_;
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

Wallet Serializer::convert2CWallet(JNIEnv *env, jobject wallet) {
    jclass clazz = env->FindClass("com/nunchuk/android/model/bridge/WalletBridge");

    jfieldID fieldId = env->GetFieldID(clazz, "id", "Ljava/lang/String;");
    auto idVal = (jstring) env->GetObjectField(wallet, fieldId);
    const char *id = env->GetStringUTFChars(idVal, nullptr);

    jfieldID fieldName = env->GetFieldID(clazz, "name", "Ljava/lang/String;");
    auto nameVal = (jstring) env->GetObjectField(wallet, fieldName);
    const char *name = env->GetStringUTFChars(nameVal, nullptr);

    jfieldID fieldTotalRequireSigns = env->GetFieldID(clazz, "totalRequireSigns", "I");
    auto total_required_signs = (jint) env->GetIntField(wallet, fieldTotalRequireSigns);

    jfieldID fieldSigners = env->GetFieldID(clazz, "signers", "Ljava/util/List;");
    auto signersVal = (jobject) env->GetObjectField(wallet, fieldSigners);
    auto signers = Serializer::convert2CSigners(env, signersVal);

    jfieldID fieldAddressType = env->GetFieldID(clazz, "addressType", "I");
    jint addressTypeVal = env->GetIntField(wallet, fieldAddressType);
    auto address_type = Serializer::convert2CAddressType(addressTypeVal);

    jfieldID fieldEscrow = env->GetFieldID(clazz, "escrow", "Z");
    auto escrow = env->GetBooleanField(wallet, fieldEscrow);

    Wallet updateWallet = Wallet(id, signers.size(), total_required_signs, signers, address_type, escrow, 0);
    updateWallet.set_name(name);

    env->ReleaseStringUTFChars(nameVal, name);
    env->ReleaseStringUTFChars(idVal, id);

    return updateWallet;
}
