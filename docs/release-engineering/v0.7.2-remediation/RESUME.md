# v0.7.2 governance remediation resume

Current phase: Remediation outcomes, maintained records, and evidence bundle are complete; publish the final post-merge documentation snapshot.
Branch: `codex/v0.7.2-remediation-closeout`
HEAD: Documentation branch is based on PR #68 merge `468436d8236248bf1f017b1a503113cd41f81d73`; refresh the exact branch SHA from GitHub when resuming.
Last completed action: PR #68 merged after all seven exact-head checks passed; active stable-tag ruleset and repository immutable-release setting were applied and read back; public erratum body, unchanged six assets, tag, APK checksum, signer, package, and version were verified; 12-file bundle and ZIP were created.
Next exact action: Check the live status of this documentation-only branch/PR, merge after its required checks pass, then refresh `main` and the final public tag, Release, assets, and APK readbacks. Do not repeat any release mutation.
Active failure: None. GitHub's live Release API/UI report `immutable=true` for v0.7.2 while its setup guide says the setting applies to future releases only; preserve this observation without generalizing.
PR: Primary remediation [#68](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/68) merged at `468436d8236248bf1f017b1a503113cd41f81d73`; this final documentation snapshot is on the current closeout branch.
CI: PASS, 7/7 checks on PR #68 head `85daba251b85995a77e1675174f4ef6946d427fd`; check live closeout PR status before acting.
GitHub governance status: Active stable-tag ruleset ID `24444174` protects matching tags against update/deletion with an empty bypass list and unrestricted creation. Repository immutable-release setting reads enabled; release ID `402346895` reads immutable true. `v0.7.2` still peels to `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`.
Human action required: NONE.
