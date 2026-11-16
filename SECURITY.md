# Security Policy

## Supported versions

Cial.ai is pre-1.0 in spirit even at v1.0.0: only the latest release is supported. Fixes land in new releases, not backports.

## Reporting a vulnerability

If you find a security problem, please report it privately before opening a public issue:

- Open a private security advisory on GitHub (Security tab, "Report a vulnerability") in this repository, or
- Contact the maintainer through the contact details on the GitHub profile of the repository owner.

Please include what you found, how to reproduce it, and the impact you believe it has. You should get a response within a few days.

## Areas that matter most

Cial.ai handles sensitive capabilities, so reports in these areas are especially welcome:

- The Accessibility Service doing anything beyond focus detection, overlay display and approved text insertion (see PRIVACY.md).
- The API key being stored insecurely, logged, or sent anywhere other than the provider the user selected.
- Dictated text or transcripts being transmitted, stored or leaked unexpectedly.
- The overlay or insertion flow being usable by another app to inject text without the user's action.

## Scope notes

- The release APK is debug-signed for sideload testing. Findings that amount to "a debug build is not a store build" are known and documented in the README.
- Speech recognition is performed by the device's speech service (usually Google's). Vulnerabilities in that system component are out of scope here; report those to its provider.
- AI output quality issues (awkward phrasing, wrong tone) are bugs, not security vulnerabilities; feel free to open a normal issue for them.
