#!/usr/bin/env python3
"""Compare APK payloads, allowing only documented signing/build metadata.

ZIP layout and APK signing blocks are outside this comparison. This does not
verify signing certificates. Default comparisons decode changed manifests and
resource tables; --strict compares every non-signature entry byte for byte.
"""

import argparse
from collections import Counter
from dataclasses import dataclass, field
import difflib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from zipfile import BadZipFile, ZipFile


ANDROID = "{http://schemas.android.com/apk/res/android}"
SIGNATURE = re.compile(r"META-INF/(?:MANIFEST\.MF|[A-Z0-9_-]{1,8}\.(?:SF|RSA|DSA|EC))\Z", re.I)
PLAY_METADATA = {
    "com.android.stamp.source": {"https://play.google.com/store"},
    # SourceStampConstants.StampType in bundletool: split and standalone APKs.
    "com.android.stamp.type": {"STAMP_TYPE_DISTRIBUTION_APK", "STAMP_TYPE_STANDALONE_APK"},
}
DERIVED_ID = "com.android.vending.derived.apk.id"
MAPPING_ID = "com.google.firebase.crashlytics.mapping_file_id"


class ComparisonError(Exception):
    """Invalid input or a required decoder could not run."""


@dataclass
class ComparisonResult:
    failures: list = field(default_factory=list)
    notes: list = field(default_factory=list)
    errors: list = field(default_factory=list)
    checked: int = 0

    @property
    def matches(self):
        return not self.failures and not self.errors


def find_tool(name, explicit=None):
    if explicit:
        candidate = shutil.which(str(explicit))
        if candidate:
            return candidate
        raise ComparisonError("Cannot execute {}: {}".format(name, explicit))
    candidate = shutil.which(name)
    if candidate:
        return candidate
    roots = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"),
             str(Path.home() / "Android/Sdk"), str(Path.home() / "Library/Android/sdk")]
    pattern = {"aapt2": "build-tools/*/aapt2",
               "apkanalyzer": "cmdline-tools/*/bin/apkanalyzer"}.get(name)
    if pattern:
        for root in filter(None, roots):
            # Prefer the newest installed SDK tool, including cmdline-tools/latest.
            candidates = sorted(Path(root).glob(pattern), key=lambda p: [
                (1, int(part)) if part.isdigit() else (0, part)
                for part in re.split(r"(\d+)", str(p))], reverse=True)
            for path in candidates:
                if path.is_file() and os.access(path, os.X_OK):
                    return str(path)
    raise ComparisonError("{} is required; put it on PATH or pass --{} PATH".format(name, name))


def run_tool(args):
    try:
        proc = subprocess.run(args, capture_output=True, text=True, timeout=120)
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise ComparisonError("Could not run {}: {}".format(args[0], exc)) from exc
    if proc.returncode:
        raise ComparisonError("{} failed (exit {}): {}".format(
            args[0], proc.returncode, (proc.stderr or proc.stdout).strip()))
    if not proc.stdout.strip():
        raise ComparisonError("{} produced no decoded output".format(args[0]))
    return proc.stdout


def dump_manifest(path, tool=None):
    return run_tool([find_tool("apkanalyzer", tool), "manifest", "print", str(Path(path).resolve())])


def dump_resources(path, tool=None):
    return run_tool([find_tool("aapt2", tool), "dump", "resources", str(Path(path).resolve())])


def normalize_manifest(xml):
    """Remove only exact, direct application metadata added by Play.

    Permissions, SDK levels, component attributes, namespaces, split settings,
    other metadata, and child order are all retained.
    """
    try:
        root = ET.fromstring(xml)
    except ET.ParseError as exc:
        raise ComparisonError("Could not parse decoded manifest: {}".format(exc)) from exc
    if root.tag != "manifest":
        raise ComparisonError("Decoded XML does not have a manifest root")
    removed = []
    for app in root.findall("application"):
        counts = Counter(node.get(ANDROID + "name") for node in app.findall("meta-data"))
        for node in list(app):
            name = node.get(ANDROID + "name")
            value = node.get(ANDROID + "value", "")
            expected = (name in PLAY_METADATA and value in PLAY_METADATA[name]) or (
                name == DERIVED_ID and re.fullmatch(r"[1-9][0-9]*", value) is not None)
            if (node.tag == "meta-data" and expected and counts[name] == 1
                    and set(node.attrib) == {ANDROID + "name", ANDROID + "value"}
                    and not len(node) and not (node.text or "").strip()
                    and not (node.tail or "").strip()):
                app.remove(node)
                removed.append("{}={}".format(name, value))
    # XML attribute order, namespace prefixes, and formatting are not content.
    # Keep expanded namespace URIs, every value and the original element order.
    for node in root.iter():
        attrs = sorted(node.attrib.items())
        node.attrib.clear()
        node.attrib.update(attrs)
        if node.text is not None and not node.text.strip():
            node.text = None
        if node.tail is not None and not node.tail.strip():
            node.tail = None
    ET.register_namespace("android", ANDROID[1:-1])
    return ET.tostring(root, encoding="unicode").replace("><", ">\n<"), removed


def normalize_resources(dump):
    """Mask only the default 32-hex Crashlytics mapping ID, not its resource.

    Retain its ID, name and configuration, and all other resources verbatim.
    A missing, malformed or differently configured mapping ID still differs.
    """
    pattern = (r'(?m)^([ \t]*resource 0x[0-9a-fA-F]+ string/' + re.escape(MAPPING_ID)
               + r'\n[ \t]+\(\) )"[0-9a-fA-F]{32}"(?=\n|$)')
    return re.subn(pattern, r'\1"<CRASHLYTICS_MAPPING_FILE_ID>"', dump)


def entries(archive):
    result = {}
    seen = set()
    for info in archive.infolist():
        if info.filename in seen:
            raise ComparisonError("Duplicate ZIP entry in {}: {}".format(archive.filename, info.filename))
        seen.add(info.filename)
        if not info.is_dir():
            result[info.filename] = info
    return result


def compareFiles(first, second):
    while True:
        a, b = first.read(128 * 1024), second.read(128 * 1024)
        if a != b:
            return False
        if not a:
            return True


def text_difference(label, first, second):
    diff = "\n".join(difflib.unified_diff(
        first.splitlines(), second.splitlines(), fromfile="first/" + label,
        tofile="second/" + label, lineterm="", n=2))
    return "{} differs:\n{}".format(label, diff)


def compare_apks(first, second, *, aapt2=None, apkanalyzer=None, strict=False):
    result = ComparisonResult()
    try:
        with ZipFile(first) as left, ZipFile(second) as right:
            a, b = entries(left), entries(right)
            if "AndroidManifest.xml" not in a and "AndroidManifest.xml" not in b:
                result.failures.append("AndroidManifest.xml is missing from both APKs")
            for index, archive, label in ((a, left, "first"), (b, right, "second")):
                for name in sorted(list(index)):
                    if SIGNATURE.fullmatch(name):
                        del index[name]
                        result.notes.append("Excluded JAR signature entry ({}): {}".format(label, name))
                    elif name == "stamp-cert-sha256":
                        # This is a certificate digest, not arbitrary app data.
                        if len(archive.read(name)) != 32:
                            result.failures.append("Invalid SourceStamp certificate digest ({}): expected 32 bytes".format(label))
                        else:
                            result.notes.append("Excluded SourceStamp certificate digest ({})".format(label))
                        del index[name]
            for name in sorted(a.keys() - b.keys()):
                result.failures.append("Only in first APK: " + name)
            for name in sorted(b.keys() - a.keys()):
                result.failures.append("Only in second APK: " + name)
            for name in sorted(a.keys() & b.keys()):
                result.checked += 1
                with left.open(a[name]) as x, right.open(b[name]) as y:
                    if compareFiles(x, y):
                        continue
                try:
                    if name == "AndroidManifest.xml" and not strict:
                        x, removed_x = normalize_manifest(dump_manifest(first, apkanalyzer))
                        y, removed_y = normalize_manifest(dump_manifest(second, apkanalyzer))
                        for label, removed in (("first", removed_x), ("second", removed_y)):
                            if removed:
                                result.notes.append("Normalized Play manifest metadata ({}): {}".format(label, ", ".join(removed)))
                        if x != y:
                            result.failures.append(text_difference(name, x, y))
                        else:
                            result.notes.append("AndroidManifest.xml: decoded content matches after the reported normalization")
                    elif name == "resources.arsc" and not strict:
                        raw_x, raw_y = dump_resources(first, aapt2), dump_resources(second, aapt2)
                        x, count_x = normalize_resources(raw_x)
                        y, count_y = normalize_resources(raw_y)
                        if raw_x != raw_y and (count_x or count_y):
                            result.notes.append("Normalized Crashlytics mapping ID values only (first: {}, second: {})".format(count_x, count_y))
                        if x != y:
                            result.failures.append(text_difference(name, x, y))
                        else:
                            result.notes.append("resources.arsc: decoded resource IDs, names, configurations and values match after the reported normalization")
                    else:
                        result.failures.append("Payload entry differs: " + name)
                except ComparisonError as exc:
                    result.errors.append("{}: {}".format(name, exc))
    except (OSError, BadZipFile, RuntimeError, NotImplementedError, ComparisonError) as exc:
        result.errors.append(str(exc))
    return result


def apk_key(path):
    """Recognize the base-module names used by bundletool and adb pm path."""
    name = path.name
    if name in ("base.apk", "base-master.apk"):
        return "base"
    if name.startswith("split_config."):
        return "config." + name[len("split_config."):-4]
    if name.startswith("base-"):
        return "config." + name[len("base-"):-4]
    return path.stem


def apk_directory(path):
    found = {}
    for apk in sorted(path.rglob("*")):
        if not apk.is_file() or apk.suffix.lower() != ".apk":
            continue
        key = apk_key(apk)
        if key in found:
            raise ComparisonError("Ambiguous APK pair {}: {} and {}".format(key, found[key], apk))
        found[key] = apk
    if not found:
        raise ComparisonError("No APK files found in " + str(path))
    return found


def compare_bundle_and_apk(first, second, *, bundletool=None, **options):
    bundle, apk = (first, second) if Path(first).suffix.lower() == ".aab" else (second, first)
    with tempfile.TemporaryDirectory(prefix="apkdiff-") as directory:
        archive = Path(directory) / "universal.apks"
        command = [find_tool("bundletool", bundletool), "build-apks", "--mode=universal",
                   "--bundle=" + str(Path(bundle).resolve()), "--output=" + str(archive)]
        # bundletool may succeed without writing to stdout.
        try:
            proc = subprocess.run(command, capture_output=True, text=True, timeout=300)
        except (OSError, subprocess.TimeoutExpired) as exc:
            raise ComparisonError("bundletool could not generate a universal APK: {}".format(exc)) from exc
        if proc.returncode:
            raise ComparisonError("bundletool failed: " + (proc.stderr or proc.stdout).strip())
        universal = Path(directory) / "universal.apk"
        with ZipFile(archive) as generated:
            if "universal.apk" not in entries(generated):
                raise ComparisonError("bundletool output does not contain universal.apk")
            with generated.open("universal.apk") as source, universal.open("wb") as target:
                shutil.copyfileobj(source, target)
        result = compare_apks(str(universal), str(apk), **options)
        result.notes.insert(0, "AAB converted to universal.apk using bundletool; comparing generated APK content, not the original AAB container")
        return result


def compareApks(first, second):
    """Compatibility for callers of the old two-file helper."""
    return compare_apks(first, second).matches


def compareApkAndBundle(first, second):
    return compare_bundle_and_apk(first, second).matches


def print_result(result, *, verbose=False):
    if not verbose:
        return
    for note in result.notes:
        print("NOTE: " + note)
    for failure in result.failures:
        print("DIFFERENCE: " + failure)
    for error in result.errors:
        print("ERROR: " + error)
    print("Compared {} common payload entries.".format(result.checked))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("first", type=Path, help="APK, AAB, or directory of APKs")
    parser.add_argument("second", type=Path, help="APK, AAB, or directory of APKs")
    parser.add_argument("-v", "--verbose", action="store_true", help="Show compared files, notes, differences and errors")
    parser.add_argument("--strict", action="store_true", help="Compare non-signature entry bytes without manifest/resource normalization")
    parser.add_argument("--aapt2", help="Path to Android SDK aapt2")
    parser.add_argument("--apkanalyzer", help="Path to Android SDK apkanalyzer")
    parser.add_argument("--bundletool", help="Path to bundletool (for AAB-to-universal-APK comparison)")
    args = parser.parse_args(argv)
    options = dict(aapt2=args.aapt2, apkanalyzer=args.apkanalyzer, strict=args.strict)
    results = []
    try:
        for path in (args.first, args.second):
            if not path.exists():
                raise ComparisonError("Input does not exist: " + str(path))
        if args.first.is_dir() and args.second.is_dir():
            a, b = apk_directory(args.first), apk_directory(args.second)
            inventory = ComparisonResult(notes=["Comparing every APK recursively; non-APK files such as toc.pb are not APK payloads."])
            for key in sorted(a.keys() - b.keys()):
                inventory.failures.append("APK only in first directory: " + str(a[key]))
            for key in sorted(b.keys() - a.keys()):
                inventory.failures.append("APK only in second directory: " + str(b[key]))
            print_result(inventory, verbose=args.verbose)
            results.append(inventory)
            for key in sorted(a.keys() & b.keys()):
                if args.verbose:
                    print("\nComparing {} <-> {}".format(a[key], b[key]))
                result = compare_apks(str(a[key]), str(b[key]), **options)
                print_result(result, verbose=args.verbose)
                results.append(result)
        elif args.first.is_file() and args.second.is_file():
            suffixes = [args.first.suffix.lower(), args.second.suffix.lower()]
            if suffixes == [".apk", ".apk"]:
                result = compare_apks(str(args.first), str(args.second), **options)
            elif sorted(suffixes) == [".aab", ".apk"]:
                result = compare_bundle_and_apk(str(args.first), str(args.second), bundletool=args.bundletool, **options)
            else:
                raise ComparisonError("Expected two APKs or one AAB and one universal APK")
            print_result(result, verbose=args.verbose)
            results.append(result)
        else:
            raise ComparisonError("Compare two files or two directories, not a mixture")
    except (ComparisonError, OSError, BadZipFile) as exc:
        if args.verbose:
            print("ERROR: " + str(exc), file=sys.stderr)
        results.append(ComparisonResult(errors=[str(exc)]))
    if any(result.errors for result in results):
        print("Comparison incomplete: required input or tool checks failed.")
        return 2
    if any(result.failures for result in results):
        print("APKs are different!")
        return 1
    print("APKs are the same!")
    return 0


if __name__ == "__main__":
    sys.exit(main())
