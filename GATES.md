# Gates: Android CI Pre-Flight Checks

- [x] G1: Project builds successfully locally
    CHECK: ./gradlew assembleDebug
    EXPECT: BUILD SUCCESSFUL
    CWD: .
  EVIDENCE: automatic-evidence=v1; definition-sha256=047258ec4f623a6c4c42b5184fafd0ed80b9e3484379e5c329e0b8a8c7eb8a25; exit=0; EXPECT=matched; output-sha256=9e1908419c9d1c68a641776bda2be465ff2a48f9ad30bf80bd56b3372b8bbe88; output-bytes=25; shell=/bin/sh; cwd=/Users/stephenweeks/Documents/Codex/OPRA-EQ-for-UAPP; path=6b53acf94803/10 entries

- [x] G2: Local Emulator UI Tests pass
    CHECK: ./gradlew connectedAndroidTest
    EXPECT: SUCCESS
    CWD: .
  EVIDENCE: automatic-evidence=v1; definition-sha256=031e479b99eec9f11ca8f809aaa74fc832b16b5a92c300e3469fb1fae4f2cdad; exit=0; EXPECT=matched; output-sha256=9e1908419c9d1c68a641776bda2be465ff2a48f9ad30bf80bd56b3372b8bbe88; output-bytes=25; shell=/bin/sh; cwd=/Users/stephenweeks/Documents/Codex/OPRA-EQ-for-UAPP; path=6b53acf94803/10 entries

- [x] G3: EW300 Reconnect Verification script passes validation
    CHECK: node scripts/verify-ew300-reconnect.mjs
    EXPECT: reconnect verification passed
    CWD: .
  EVIDENCE: automatic-evidence=v1; definition-sha256=a92558e8eef9ffc9d2f36a4b4f0ef6cee0d61931c12786cf29026897af39adf2; exit=0; EXPECT=matched; output-sha256=4467a3ef86c724dd181bf4b869f40961c287f9796e9aaf5a2ea942754a3f5c55; output-bytes=252; shell=/bin/sh; cwd=/Users/stephenweeks/Documents/Codex/OPRA-EQ-for-UAPP; path=6b53acf94803/10 entries
