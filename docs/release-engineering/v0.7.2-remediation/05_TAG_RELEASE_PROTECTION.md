# F2: Stable tag protection and Release immutability

Audit date: 2026-10-03. This is a read-only governance investigation. No GitHub settings, rulesets, refs, releases, or assets were changed.

## Live GitHub state

Repository: `weekssa/OPRA-EQ-for-UAPP`, public, default branch `main`. The live `main` head at the audit was `daef0560bc120aebfe9d8910a1b867a9f9390c15`.

The connected GitHub ruleset read returned exactly one applicable ruleset, [Protect main](https://github.com/weekssa/OPRA-EQ-for-UAPP/rules/20903930). It is active, targets `~DEFAULT_BRANCH`, and has `deletion` and `non_fast_forward` rules. Its bypass list is empty and the API reports `current_user_can_bypass: never`. The ruleset targets a branch, not tags. No applicable tag ruleset was returned by the repository ruleset collection, including inherited rulesets.

The latest public release is [EQ Library v0.7.2](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.2), release ID `402346895`, published at `2026-10-03T05:32:08Z`, non-draft, non-prerelease, and latest. GitHub's release API reports `immutable: false`. The release APK asset digest is `sha256:efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`.

The `v0.7.2` ref is an annotated tag object `d04a10895d56dcc96218ea3056bc5f593ea9b2fe` that peels to the exact release source `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`. Its message binds candidate run `37099431204`, artifact `11265653006`, and archive SHA-256 `dc2607ad3b8c43aae4b0d41502ff5f13ce03d8b7b635511941f8271be255ab33`. GitHub reports the tag object as unsigned (`verification.verified: false`, reason `unsigned`); this is separate from the signed APK and its recorded provenance. Main has advanced since the release tag, which remains pinned to the exact release source.

Live records: [ruleset collection](https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/rulesets?includes_parents=true), [Protect main ruleset](https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/rulesets/20903930), [v0.7.2 release](https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/releases/402346895), [latest release](https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/releases/latest), [v0.7.2 tag ref](https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/git/ref/tags/v0.7.2), [annotated tag object](https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/git/tags/d04a10895d56dcc96218ea3056bc5f593ea9b2fe), and [main branch](https://api.github.com/repos/weekssa/OPRA-EQ-for-UAPP/branches/main).

## Narrow tag ruleset proposal

Add one active repository tag ruleset named `Protect stable SemVer tags` with this scope and behavior:

```json
{
  "target": "tag",
  "enforcement": "active",
  "conditions": {
    "ref_name": {
      "include": ["refs/tags/v[0-9]*.[0-9]*.[0-9]*"],
      "exclude": ["refs/tags/*-*"]
    }
  },
  "rules": [
    { "type": "update" },
    { "type": "deletion" }
  ],
  "bypass_actors": []
}
```

Leave tag creation unrestricted. The release workflow can create each new version ref because no creation rule is selected; it does not need a bypass actor to create new tags. The update rule blocks moving an existing matching ref, and the deletion rule blocks deleting it. Use an empty bypass list: do not grant bypass to repository admins, a `GitHub Actions` integration, or another role or app. GitHub identifies repository admins, maintain/write roles, teams, and GitHub Apps as possible bypass actors. A repository admin can still edit or disable the ruleset itself, so the empty list prevents routine bypass rather than removing administrative control.

This is the narrowest practical GitHub target for the repository's current stable naming form, `vMAJOR.MINOR.PATCH`, while excluding hyphenated prerelease tags. GitHub uses `fnmatch` for tag targeting, not a regular expression: `[0-9]` requires one digit and `*` matches any string except a slash, so this pattern can also match malformed version-shaped names. It may overprotect such names; it does not validate SemVer. The `*-*` exclusion also omits any future valid build-metadata tag whose metadata contains a hyphen. Keep or add exact version validation in release-promotion logic; do not rely on this glob to validate SemVer. GitHub ruleset targeting supports inclusion and exclusion patterns, and the API represents tag refs with the `refs/tags/` prefix. See [Creating rulesets for a repository](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/creating-rulesets-for-a-repository) and [REST API endpoints for rules](https://docs.github.com/en/rest/repos/rules?apiVersion=2026-03-10).

The repository is public. GitHub documents rulesets for public repositories on GitHub Free; creating, editing, or deleting a repository ruleset requires repository admin access or a custom role with `edit repository rules`. The live repository API response reports `permissions.admin: true` for the connected identity. The existing no-bypass `Protect main` ruleset is consistent with the proposed no-bypass tag rule. The tag rules themselves are documented as `Restrict updates` and `Restrict deletions`; leaving `Restrict creations` off is what keeps new version tags creatable. See [Available rules for rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets).

## Immutable Releases and the v0.7.2 limit

GitHub's release record directly confirms that v0.7.2 is not immutable. Enabling the repository's release-immutability setting now cannot change that: GitHub explicitly limits the setting to future releases. Therefore v0.7.2's existing release assets will remain outside the immutable-release lock. A stable-tag ruleset can protect the matching `v0.7.2` ref from ordinary updates and deletions once added, but it does not lock release assets. Even for immutable releases, GitHub leaves the title and release notes editable.

I could not read the repository-level setting in this investigation. The connected GitHub fetch wrapper rejected `GET /repos/weekssa/OPRA-EQ-for-UAPP/immutable-releases` as outside its approved read endpoints; `gh` is not installed in this worktree. The public repository metadata response does not expose that setting. Consequently, the current setting state is unknown; the release object's `immutable: false` state is known. GitHub's documented check endpoint requires admin read access and `Administration` repository permission (read); enabling it requires repository admin access and `Administration` repository permission (write). The repository settings UI also documents the owner/admin route. See [REST API endpoints for repositories](https://docs.github.com/en/enterprise-cloud@latest/rest/repos/repos#check-if-immutable-releases-are-enabled-for-a-repository) and [Preventing changes to your releases](https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/establish-provenance-and-integrity/prevent-release-changes).

For future releases, enable release immutability before the next publication, create the release as a draft, upload and verify all assets, then publish. GitHub's immutable-release behavior locks the tag and release assets after publication and generates a release attestation. The release notes/title may still be edited. See [Immutable releases](https://docs.github.com/en/code-security/concepts/supply-chain-security/immutable-releases) and [Managing releases in a repository](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository).

## Permission and verification boundary

No tag ruleset was created, and I did not enable or disable release immutability. The exact `immutable: false` state for v0.7.2 is confirmed from the live release record. The repo-wide immutable-release toggle was not independently read, and should not be recorded as either enabled or disabled without the admin-read endpoint or settings UI. Ruleset and immutable-release setting mutations require separate authorized write operations; this report performs neither.
