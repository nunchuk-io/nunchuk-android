#!/usr/bin/env python3
import sys
from zipfile import ZipFile

def compareFiles(first, second):
    while True:
        firstBytes = first.read(4096)
        secondBytes = second.read(4096)
        if firstBytes != secondBytes:
            return False

        if firstBytes == b"" and secondBytes == b"":
            break

    return True

def remove_prefix(text, prefix):
    if text.startswith(prefix):
        return text[len(prefix):]
    return text

def compareApkAndBundle(first, second):
    FILES_TO_IGNORE = [
            "resources.arsc", 
            "stamp-cert-sha256", 
            "assets/dexopt/baseline.prof", 
            "assets/dexopt/baseline.profm",
            "AndroidManifest.xml",
            ]
    if first.endswith("apk"):
        apk = first
        bundle = second
    elif second.endswith("apk"):
        apk = second
        bundle = first

    apkZip = ZipFile(apk, 'r')
    bundleZip = ZipFile(bundle, 'r')

    firstList = list(filter(lambda info: info.filename not in FILES_TO_IGNORE, apkZip.infolist()))
    secondList = list(filter(lambda secondInfo: secondInfo.filename not in FILES_TO_IGNORE, bundleZip.infolist()))

    for apkInfo in firstList:
        if (apkInfo.filename.startswith("res/")):
            continue

        found = False
        for bundleInfo in secondList:
            fileName = bundleInfo.filename
            fileName = remove_prefix(fileName, "base/root/")
            fileName = remove_prefix(fileName, "base/dex/")
            fileName = remove_prefix(fileName, "base/manifest/")
            fileName = remove_prefix(fileName, "base/")
            if (fileName.startswith("BUNDLE-METADATA")):
                fileName = "META-INF" + remove_prefix(fileName, "BUNDLE-METADATA/")
            if fileName == apkInfo.filename:
                found = True
                firstFile = apkZip.open(apkInfo, 'r')
                secondFile = bundleZip.open(bundleInfo, 'r')
                if compareFiles(firstFile, secondFile) != True:
                    print("APK file %s does not match" % apkInfo.filename)
                    return False
                break

        if found == False:
            print("file %s not found in APK" % apkInfo.filename)
            return False

    return True
    

def compareApks(first, second):
    FILES_TO_IGNORE = [
            "META-INF/MANIFEST.MF", 
            "META-INF/CERT.RSA", 
            "META-INF/CERT.SF", 
            "META-INF/BNDLTOOL.SF",
            "META-INF/BNDLTOOL.RSA",
            "stamp-cert-sha256",
            "resources.arsc", 
            "res/xml/splits0.xml",
            "AndroidManifest.xml",
            ]

    firstZip = ZipFile(first, 'r')
    secondZip = ZipFile(second, 'r')

    firstList = list(filter(lambda firstInfo: firstInfo.filename not in FILES_TO_IGNORE, firstZip.infolist()))
    secondList = list(filter(lambda secondInfo: secondInfo.filename not in FILES_TO_IGNORE, secondZip.infolist()))

    if len(firstList) != len(secondList):
        print("APKs has different amount of files (%d != %d)" % (len(firstList), len(secondList)))
        return False

    for firstInfo in firstList:
        found = False
        for secondInfo in secondList:
            if firstInfo.filename == secondInfo.filename:
                found = True
                firstFile = firstZip.open(firstInfo, 'r')
                secondFile = secondZip.open(secondInfo, 'r')

                if compareFiles(firstFile, secondFile) != True:
                    print("APK file %s does not match" % firstInfo.filename)
                    return False

                secondList.remove(secondInfo)
                break

        if found == False:
            print("file %s not found in second APK" % firstInfo.filename)
            return False

    if len(secondList) != 0:
        for secondInfo in secondList:
            print("file %s not found in first APK" % secondInfo.filename)
        return False

    return True

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: apkdiff <pathToFirstApk> <pathToSecondApk>")
        sys.exit(1)

    first = sys.argv[1]
    second = sys.argv[2]

    isBothApk = first.endswith("apk") and second.endswith("apk")
    isBundle = first.endswith("aab") or second.endswith("aab")

    if first == second or (isBothApk and compareApks(first, second)) or (isBundle and compareApkAndBundle(first, second)):
        print("APKs are the same!")
    else:
        print("APKs are different!")
