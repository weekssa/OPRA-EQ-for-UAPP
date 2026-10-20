# Gates: repository foundation and v0.8.3 consolidation

OWNS: AGENTS.md, README.md, CONTRIBUTING.md, docs/PROJECT_STATE.md, docs/GOALS_AND_SCOPE.md, docs/OPERATING_RUNBOOK.md, docs/KNOWN_ISSUES.md, docs/CHATGPT_PROJECT_RUNBOOK.md, docs/maintenance/2026-10-20-repository-foundation/**, tools/validate-repository-foundation.py, .github/PULL_REQUEST_TEMPLATE.md, .github/workflows/android-ci.yml, .github/workflows/android-ci-exact-head.yml

Scope: consolidate active project guidance and validate it without changing application behavior.

- [x] G1: original checkout and project history have a verified private recovery copy
  EVIDENCE: Full-copy manifest reports 16,383 identical path entries and no content/path/type/metadata differences; `git fsck --full` exits 0 and a separate full-copy restoration check preserves HEAD/status/shallow state. Bundle verifies against source for 291 refs but does not independently clone due shallow-history boundary. Private audit records E-002/E-003/E-009.

- [x] G2: canonical and task maintenance docs have no missing relative links or contradictory current release tuple
  CHECK: python3 tools/validate-repository-foundation.py
  EXPECT: repository foundation validation passed
  EVIDENCE: `python3 tools/validate-repository-foundation.py` passed after adding task maintenance docs to link scanning.

- [x] G3: production application source and build/release identity are unchanged from branch base
  CHECK: python3 -c 'import subprocess; subprocess.run(["git", "diff", "--quiet", "origin/main", "--", "app/src", "app/build.gradle.kts", "app/src/main/AndroidManifest.xml"], check=True); print("application source unchanged")'
  EXPECT: application source unchanged
  EVIDENCE: checked `app/src`, `app/build.gradle.kts`, and `app/src/main/AndroidManifest.xml` against `origin/main`; no differences.

- [x] G4: whitespace and conflict-marker checks pass
  CHECK: python3 -c 'import subprocess; subprocess.run(["git", "diff", "--check"], check=True); print("whitespace validation passed")'
  EXPECT: whitespace validation passed
  EVIDENCE: `git diff --check` passed.

- [x] G5: applicable Android unit tests, lint, and debug assembly pass
  CHECK: ./tools/codex-android :app:testDebugUnitTest lintDebug assembleDebug
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: `./tools/codex-android :app:testDebugUnitTest lintDebug assembleDebug` completed with `BUILD SUCCESSFUL` in 3m 24s. Existing deprecation, Kotlin, and native-symbol-strip warnings were emitted; no failures.

- [x] G6: reviewer confirms factual boundaries and sensitive files are excluded
  EVIDENCE: Independent read-only review found no remaining actionable findings; reviewed changes contain no local filesystem paths or raw private reports and no application/build/release behavior changes.
