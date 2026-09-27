# Owner approval — exact UX review candidate

Source branch: `codex/b354167-ux-focus`
Approved source: `e2afcca38b1b31c333b67af9499bb49d49f0e428`
Base: `b3541670d095b2fa875e96a0a0b0730da154a1d9`
Decision date: 2026-09-26

## Decision

Approved: move `main` to the exact approved UX commit so the existing secure signing workflow can produce a signed review candidate. This approval covers candidate build, exact-source CI, provenance verification, and publication of the signed review candidate only.

The approved UX scope is complete: Settings copy and row/headings accessibility, JA11 headset-switch row accessibility, and active DEVICE “Applying…” presentation. No further UX source change is approved.

No public release, tag, announcement, hardware-support claim, final go-live, further merge, or hardware mutation is approved. Owner will perform one complete Pixel 9 UX review on the exact signed candidate before any final signoff.

## Follow-up approval — Flash feedback consistency

After reviewing the exact `e2afcca` signed candidate on the Pixel 9, the owner reported success but identified inconsistent Flash feedback across Black Pearl, JA11, and EW300. The owner approved one presentation-only consistency pass proposed in this task: retain pre-Flash confirmation; use one compact, non-modal top-of-screen status across Flash entry points and My DAC; show progress, observed reconnect/verification, verified completion, and uncertain/failure states truthfully; remove duplicate transient Flash success messages; and provide concise TalkBack announcements. The owner instructed the team to stay within the existing token-light Sol → one Luna High → same Sol review process.

This narrow approval supersedes the earlier sentence that no further UX source change was approved. It does not authorize navigation, hardware controls, USB/session or transaction changes, a new hardware claim, main integration, signing, publication, or further UX audit for this follow-up.

## 2026-09-26 Pixel 9 feedback follow-up

After reviewing signed source `2d4e8d6b33a937fe555463e1a947aa6f0df84ee6`, the owner reported six focused issues and approved the proposed presentation repair. The owner then directed this task to continue the UX changes and prepare a workflow for another task. The current source has no JA11 EQ editor or Apply path, so restoring a functional JA11 Edit action requires a separate design and hardware-write approval. This UX candidate defers that item and does not add a dead-end button or new JA11 transaction. The other approved feedback corrections proceed under the existing source and hardware boundaries. See `03-operation-feedback-next-task-handoff.md` for exact status.

The owner subsequently confirmed: defer the JA11 edit fix entirely outside this UX task flow and keep the current work focused on UX feedback. This resolves the pending JA11 scope choice; no JA11 editor implementation or additional UX discovery is authorized here.

## Follow-up resolution — Black Pearl result presentation

After the Sol review identified that the first Flash-feedback implementation could only show Black Pearl's plain-language callback as unverified, the owner approved resolving that specific presentation gap and retaining the `e2afcca` signed APK as the comparison baseline. The UI may consume the existing typed Black Pearl Flash result to distinguish all reports sent from a failed or unavailable transfer. It must not call Black Pearl's sent result verified persistence, because that operation does not provide a final hardware readback. This approval remains within the presentation-only Flash feedback process and does not authorize a new build, push, merge, signing run, or hardware action.
