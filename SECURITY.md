# Security Policy

## Supported versions

Security fixes are applied to the current development/release line. Until a stable `v1.0.0` exists, only the latest published `0.x` release should be considered supported.

## Reporting a vulnerability

Please do **not** disclose a security vulnerability, credential, signing key, private file, or exploit details in a public GitHub Issue.

When GitHub private vulnerability reporting is available for this repository, use the repository's **Security → Report a vulnerability** flow. If that option is not available, contact the repository owner privately through the GitHub account associated with this project before sharing sensitive technical details.

For ordinary non-sensitive bugs, use GitHub Issues after the repository is public.

## Secrets and release signing

Signing keys, keystores, passwords, API tokens, and other credentials must never be committed to this repository. Android release signing must use a stable signing identity stored outside source control.
