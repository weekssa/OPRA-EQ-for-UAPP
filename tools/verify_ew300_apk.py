"""Fail the candidate gate if the diagnostic launcher is absent from DEX definitions."""

import argparse
import hashlib
import re
import struct
import zipfile

LAUNCHER = b"Lcom/weekssa/opraeqforuapp/diagnostics/Ew300UsbDiscoveryActivity;"


def defined_classes(dex):
    if not dex.startswith(b"dex\n"):
        raise ValueError("Unsupported DEX header")
    string_count, string_offset, type_count, type_offset = struct.unpack_from("<4I", dex, 56)
    class_count, class_offset = struct.unpack_from("<2I", dex, 96)
    for i in range(class_count):
        type_index = struct.unpack_from("<I", dex, class_offset + i * 32)[0]
        if type_index >= type_count:
            raise ValueError("Invalid DEX type index")
        string_index = struct.unpack_from("<I", dex, type_offset + type_index * 4)[0]
        if string_index >= string_count:
            raise ValueError("Invalid DEX string index")
        offset = struct.unpack_from("<I", dex, string_offset + string_index * 4)[0]
        # Skip the ULEB128 UTF-16 length; class descriptors here are ASCII.
        while dex[offset] & 128:
            offset += 1
        offset += 1
        yield dex[offset:dex.index(b"\0", offset)]


def verify(apk):
    with zipfile.ZipFile(apk) as archive:
        dex_files = [name for name in archive.namelist() if re.fullmatch(r"classes\d*\.dex", name)]
        if not any(LAUNCHER in set(defined_classes(archive.read(name))) for name in dex_files):
            raise ValueError("Diagnostic launcher is NOT defined in APK DEX; installation would crash on launch")
    with open(apk, "rb") as source:
        digest = hashlib.file_digest(source, "sha256").hexdigest()
    print(f"PASS: diagnostic launcher class defined in APK\nAPK SHA-256: {digest}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk")
    verify(parser.parse_args().apk)
