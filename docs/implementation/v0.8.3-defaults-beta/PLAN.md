# v0.8.3 JA11 defaults beta execution plan

## Frozen intake

Repository `weekssa/OPRA-EQ-for-UAPP`; base `575dab59fe774d3683a3714ca490a334f1b20764`; branch `codex/v0.8.3-defaults-beta`. The old worktree/branch and PR #96 are preserved. The current v0.8.2 source is `9983edf0b7fe437aeeec9ac3dcb3864705afa16a`; the architecture audit found no new application/build/workflow changes between it and this base beyond catalog/docs.

## Execution order

1. Add one repository transaction that reacquires the shared JA11 gate after the confirmation, compares the fresh current snapshot to the reviewed baseline, writes only a changed target once, verifies it, then performs the original-state-aware mic round trip with a verified replacement after each mic write. Stop at the first uncertain phase. Expose truthful counts/stage/result and never issue a Save/UAC/EQ-data/Apply command from this operation.
2. Extend the ViewModel so the full transaction is owned by `viewModelScope`, stays busy through both replacement waits, blocks or serializes other mutations, and publishes only verified final state. Keep ordinary setters runtime-only.
3. Update only JA11 volume and program dialogs: volume keeps its staged value; program becomes staged with separate hardware Current marker; both get Cancel, changed-only Apply, and Set as Default. Set as Default shows the reviewed pair and confirmation. Cancel/dismiss sends no command. Recheck and refresh the pair if it changed before confirmation.
4. Add an isolated release-derived `beta` build type with package `com.weekssa.opraeqforuapp.v083beta`, name `0.8.3-beta.1`, code 14, local test signer and source provenance. Preserve stable release task/output paths and production update behavior; disable the stable update check/banner in private beta only. Enable instrumentation for the beta variant.
5. Add deterministic repository/session/ViewModel/UI tests for success, no-write paths, boundary values, all programs, both original mic states, current-pair drift, every reconnect failure leg, no replay/forbidden command, concurrency, navigation, and truthful result states. Run applicable release, API 26, modern emulator, R8 and exact-head gates.
6. Freeze exact app/test APK bytes and provenance before requesting phone readiness. Run only the bounded wireless-ADB session authorized in the handoff, pausing for owner readiness, any unavoidable Android permission/unlock, and the explicit physical JA11 unplug/replug. Keep the exact app/data installed on PASS or FAIL.
7. Leave the result and beta for owner manual acceptance. Do not publish until the owner identifies the candidate as passing and directs publication. That instruction triggers the pre-authorized main integration and exact official signing/promotion chain.

## Evidence rules

Use J034 only as failure context for the superseded native-Save implementation; do not replay it or transfer its result to this candidate. Existing owner-reported UAC/mic retention and old volume/program results remain separately labeled and do not qualify this new sequence. App-recorded command counts are not USB bus counts. A UI `Current` marker, reconnect, app restart, or simulation does not prove true power-loss persistence.
