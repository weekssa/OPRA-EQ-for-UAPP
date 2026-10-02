# v0.7.2 stabilization mission

Create a safe stabilization release from the immutable v0.7.1 source. The verified starting point is tag v0.7.1 at commit c48f6a5daa08a5e03475b2e415fe80b41d3357db, with versionName 0.7.1 and versionCode 8. The target is versionName 0.7.2 and versionCode 9.

The release scope is evidence-backed corrections, version-driven release automation, stronger automated regression coverage, an independent investigation of the DSP response/headroom sampling concern, complete local and remote release verification, and exact signed-artifact provenance. Preserve existing USB/DAC protocols and do not write to real DAC hardware.

Do not turn this into a broad UI, navigation, ViewModel, dependency-upgrade, or v0.8 project. Do not infer hardware behavior or support from emulator, CI, chipset, or prior candidate evidence. Do not change application identity, SDK levels, protocol constants, signer identity, or hardware authorization without an independently proven requirement.

The owner authorized branch creation, tool/skill setup, tests, pushes, PR creation, merge when protected checks allow, signed-candidate workflow, immutable v0.7.2 tagging, and public publication only after all applicable gates pass. Never bypass protection, expose signing credentials, retag an existing release, or publish a candidate whose exact source, APK, checksum, package/version, signer, signature, alignment, manifest, or provenance does not verify.

The durable filesystem and GitHub evidence are authoritative. On resume, read every control-plane file in this directory, reconcile it with Git and GitHub, then continue from the first incomplete checklist item. Do not repeat irreversible release operations when current remote evidence proves they already completed.
