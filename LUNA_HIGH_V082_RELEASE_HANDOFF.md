# Luna 6.0 High — finish and publish the qualified v0.8.2 candidate

Use this as the initial prompt for a new task in this project. The handoff was prepared by Sol 6.1 on 2026-10-13 America/Chicago. No new task or publication was started by the preparing chat.

## Owner authority and roles

The owner said, “I tested and confirmed it's working as expected,” then requested a Markdown handoff “for Luna Hight to finish and publish where we left off here.” This authorizes the follow-on task to finish release closeout and publish **this exact qualified v0.8.2 candidate**, including the necessary stable tag and latest-release promotion after the existing gates pass. Routine in-scope approvals/merges were previously pre-granted. Do not ask for publication approval again for this tuple. This supersedes older “publication not authorized” mission checkpoints only for this bounded follow-on task.

Use **Luna 6.0 High** for release execution and workhorse tasks. Retain Sol 6.1 orchestration/final verification as previously instructed; keep its involvement bounded to one final release decision. If the new task runs Luna as primary, use a narrowly scoped Sol 6.1 pre-publication verification of the exact tuple, live guards, evidence limits and intended release action. Do not reopen the investigation or spawn a large team. Durable accepted evidence is shared context.

Phone access is closed. Do not discover, connect to, query, install onto, launch apps on or otherwise interact with the Pixel/JA11. No new physical test, Flash, settings change or power cycle is needed. Preserve diagnostic PR93 and historical evidence. Never commit raw phone captures, identity, private reports, credentials or signing material.

## Read once; use current evidence

Repository: `weekssa/OPRA-EQ-for-UAPP`.

Starting checkout: `/Users/stephenweeks/.codex/worktrees/6a6b/OPRA-EQ-for-UAPP`, branch `codex/ja11-postflash-reconstruction`. Bug-fix completion checkpoint: `25f43b81df3a42c59a44eaefc23ee1eb318abcdc`. This handoff and the later owner authority are subsequent documentation-only checkpoints on that branch, separate from the frozen production source.

Read root AGENTS.md and docs/CHATGPT_PROJECT_RUNBOOK.md, then these curated files rather than repeating repository-wide analysis:

- agent-runs/ja11-postflash/STATE.md, MORNING_HANDOFF.md and the J031/final entries of EVIDENCE.md; RUNBOOK.md's latest owner overrides.
- Current main versions of .github/workflows/promote-signed-release.yml and tools/promote_release_candidate.py; docs/releases/v0.8.2.md; the current public-release checklist, JA11 validation ledger, capability matrix and status documents for closeout.

If the exact v0.8.2 release is already published when you resume, independently verify that same tuple and finish closeout; do not recreate or republish it.

Refresh remote source first. The preserved fix worktree is `/Users/stephenweeks/.codex/worktrees/ja11-flash-owner-fix/OPRA-EQ-for-UAPP` at reviewed pre-merge a39965d3; its older public checklist is stale and is not live release truth. Use an isolated documentation/release worktree, inspect unrelated changes, and never reset/clean them. The owner explicitly requested this one additional handoff file; keep the original four canonical mission files and avoid extra permanent reports.

## Immutable qualified tuple

| Field | Value |
| --- | --- |
| Production source | `9983edf0b7fe437aeeec9ac3dcb3864705afa16a` |
| APK SHA-256 | `df1ac78e7462da588e01f4aa39f5f23d0c05797d27eb2222d0650ce319c5d6e3` |
| Package / version / code | com.weekssa.opraeqforuapp / 0.8.2 / 13 |
| Signer SHA-256 | `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747` |
| Official signer run / artifact | 38474740277 / 11853910967 |
| Artifact ZIP SHA-256 | `c82b5490286d27cd3616740d6f9d8ce51c9f3ac598b1f40dac87ecaac1a76b88` |
| Artifact expiry | 2027-01-11T22:00:29Z |
| Fix PR | https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/94 — merged |
| Exact-source CI | 38473044839: 777 unit, 68 API35 UI, API26, lint/build/R8, exact CodeQL PASS |
| Exact signed install/upgrade | 38475878287 PASS; verifier workflow c5be0d9af579a8ba264884db2f13f81ad6785e1a is separate from APK source |
| Physical record | J031: original Explorer/LoboNautics route, one owner Flash, same-process EQ/DEVICE recovery, Edit/navigation work, baseline unchanged, cleanup complete |

Frozen artifact directory: `/Users/stephenweeks/Downloads/JA11-v0.8.2-candidate-9983edf0b7fe437aeeec9ac3dcb3864705afa16a/`; APK is EQ-Library-v0.8.2.apk, original bundle is signed-artifact.zip. Preserve bytes; do not rebuild, re-sign or silently substitute another source/artifact.

Private evidence directories, already verified:

- /Users/stephenweeks/.codex/evidence/ja11-main-9983-20261013/
- /Users/stephenweeks/.codex/evidence/ja11-signed-9983-20261013/
- /Users/stephenweeks/.codex/evidence/ja11-v082-physical-20261013/

All 23 off-phone gates pass. Fresh adversarial Luna High source review and independent artifact review approve. J031 review is PASS_WITH_LIMITS: visible convergence/process continuity are proven; raw packet/write/Save/replay counts, internal generations and VM identity remain unmeasured. Keep those limits. The later owner confirmation is owner-reported acceptance with no new telemetry or separately established test tuple.

## Execute in this order

1. Record the owner confirmation and follow-on authority; inspect current remote main, PR94, workflow/run/artifact metadata, existing tag/release/drafts and release notes. At handoff preparation, main was exactly `9983edf0b7fe437aeeec9ac3dcb3864705afa16a`, latest immutable public release was v0.8.1 (411100198), and candidate11853910967 was unexpired with matching digest. These are snapshots, not future assumptions.
2. Prepare truthful public notes, install/upgrade/checksum/rollback guidance and documentation closeout. The existing versioned notes are docs/releases/v0.8.2.md. Prepare documentation changes in a separate branch, but **do not merge anything into main before promotion**. The publisher requires dispatch SHA and live main to equal the signed candidate source through verify/tag/publish; even a docs-only merge would intentionally invalidate that guard. Verify the existing notes meet the approved scope; if they need a source-bound change before publication, treat that as a material blocker rather than changing the frozen candidate.
3. Recompute local/API archive/APK hashes and verify source marker, package/version/code, pinned signature, alignment/R8/provenance. Obtain the bounded Sol final verification. Do not repeat passed broad source checks without a material change; the publisher's mandatory hosted checks still run normally.
4. If live main remains exactly `9983edf0b7fe437aeeec9ac3dcb3864705afa16a` and no tag/release conflict exists, use the existing trusted promotion workflow. This is the publication action; it verifies the archive, runs signed API35 upgrade/clean-install gates, creates the annotated tag and publishes with make_latest=true. It has no APK build/re-sign path. Use the available GitHub CLI (currently /Users/stephenweeks/.local/bin/gh-v2.102.0):

```sh
/Users/stephenweeks/.local/bin/gh-v2.102.0 workflow run promote-signed-release.yml \
  --repo weekssa/OPRA-EQ-for-UAPP --ref main \
  -f tag=v0.8.2 \
  -f candidate_run_id=38474740277 \
  -f candidate_artifact_id=11853910967
```

5. Monitor actual verify/smoke/tag/publish jobs and preserve failures. Resolve ordinary hosted emulator/artifact routing blockers without weakening assertions or publisher guards. Inspect partial tag/draft/release state before any retry; never blindly rerun a publication mutation or overwrite an immutable release. If main drifted, provenance changed, required notes require a source change, or an incompatible publication already exists, stop with that concrete blocker. Do not force the old candidate through, auto-create a replacement candidate, rewrite history, or invent a different signing/publishing path.
6. Independently download and verify the public tag/release and all six assets: APK, APK.sha256, candidate-manifest.json, apksigner-verification.txt, zipalign-verification.txt, release-provenance.json. Confirm tag peels to `9983edf0b7fe437aeeec9ac3dcb3864705afa16a`, public APK matches `df1ac78e7462da588e01f4aa39f5f23d0c05797d27eb2222d0650ce319c5d6e3`/signer/package/version/code, metadata and provenance bind the original run/artifact, body matches curated notes, and /releases/latest plus existing app-update metadata identify v0.8.2. Verify immutable/stable/non-draft/non-prerelease status. R8 mapping remains private. Preserve public v0.8.1 and diagnostics.
7. **After successful verified publication**, finish/merge documentation-only closeout with applicable exact-head checks: current release checklist/runbook, JA11 ledger/matrix/status, changelog/README where their facts changed, owner acceptance, J031 limits, exact public assets/provenance and rollback guidance. Later documentation SHAs do not become the APK source. Keep history append-only; do not fabricate old hardware or test evidence. Attach any created PR to the new task and push all checkpoints.

Done means: exact qualified bytes publicly verified, correct stable tag/latest/update metadata, synchronized release/evidence docs, preserved history, clean understandable worktree, and a brief owner report with release URL and exact provenance. Continue autonomously through that outcome; request input only for a genuinely material blocker, not routine engineering choices.
