# v0.7.2 governance remediation test matrix

| Surface | Check | Expected evidence | Status |
|---|---|---|---|
| Promotion readback | Focused `tools/test_promote_release_candidate.py` cases | Matching name/body pass; changed name/body fail in helper and final publication flow; draft metadata fails before return; only one optional final LF is accepted | PASS, 48 tests |
| Promotion contract | `python3 tools/promote_release_candidate.py check-contract` | Contract marker emitted | PASS |
| Repository Python | `python3 -m unittest discover -s tools -p 'test_*.py'` under bundled Python 3.12.14 | All tests pass | PASS, 247 tests |
| GitHub workflows | `actionlint` v1.7.12 | Exit 0 | PASS |
| Shell scripts | `shellcheck tools/*.sh` v0.11.0 | Exit 0 | PASS |
| Diff integrity | `git diff --check` | Exit 0 | PASS at local verification; final-head verification pending |
| Product boundary | `git diff --name-only origin/main...HEAD` and exact diff inspection | No app-functional, DSP, DAC protocol, USB authorization, version, or signing changes | Pending exact committed diff review |
| Release integrity | Tag lookup, Release API, fresh APK download, signer and version readback | Exact audited tag and APK identity retained | Pending |
