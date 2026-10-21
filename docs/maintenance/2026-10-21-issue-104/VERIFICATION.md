# Issue #104: installed v0.8.3 provenance verification

Date: 2026-10-21  
Repository base: `origin/main` `52eb6fe17ae5df32bb80b427beb25cb2da0c2c9a`  
Scope: read-only identity verification of the installed OPRA-EQ package on the owner's physical Pixel 9.

## Device and package evidence

- `adb devices -l` identified a physical `Pixel_9` target in state `device`; `emulator-5554` was separately identified as an emulator. The wireless serial and private network details are intentionally omitted.
- The owner authorized read-only inspection of the physical device. Only package `com.weekssa.opraeqforuapp` was queried.
- Package metadata: versionName `0.8.3`, versionCode `14`.
- Package Manager reported one installed APK path, the package's `base.apk`.
- SHA-256 was computed on-device for that installed APK without transferring or extracting it: `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`.
- Package Manager's limited metadata output did not provide a signer certificate SHA-256. No APK extraction was performed.

## Public release comparison

The live public v0.8.3 release APK digest is `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`, an exact match to the on-device digest. The maintained signed-release record ties those bytes to package `com.weekssa.opraeqforuapp`, versionName `0.8.3`, versionCode `14`, signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and source commit `899755507f812c940f3ab9381a856c80c6030885`.

Conclusion: **HIGH confidence** that the installed APK is byte-for-byte identical to the public signed v0.8.3 release artifact. The signer identity follows from that exact artifact match and its verified release provenance; the certificate fingerprint was not independently read from the installed package. The installed app did not expose or prove its source revision directly. Matching version labels alone would not establish this conclusion; the full APK SHA-256 match does.

## Boundaries and record

No private app data, unrelated applications, personal/device serial identifiers, or DACs were inspected. No app or device setting was changed. No APK was extracted or transferred. Physical-device inspection authorization was limited to the 2026-10-21 read-only session.

Issue #104 remains open pending this documentation draft and owner review. Do not merge without owner approval. Approval applies to the reviewed PR head; if the reviewed head changes, obtain fresh owner approval before merging.
