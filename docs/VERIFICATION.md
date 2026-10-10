# Verification and known limits

## Automated checks

The first public beta passes **90 JVM tests across 16 suites**, checked locally with JDK 21, the included Gradle wrapper and Android SDK 37. The JVM suites cover:

- Category overlap, thin/repeated/empty pages, cursor loops, blank final cursors, fallback overlap, missing broadcaster IDs, large identity sets, cancellation and retry.
- Full supplied-tag matching, rule/allow-list scoping, Unicode matching, emoji sequences, validation and neutral defaults.
- Concurrent filter-cache access and engine/rule pairing.
- Proxy preset ordering and seed behavior.
- Live buffer/offset policies and Media3 LoadControl contract forwarding.
- Swipe selection, Twitch URL validation, channel-points parsing/submission gates and release-feed parsing.

Android lint and the optimized R8 release build are part of the local checks. The lint report has **zero errors and 22 reviewed warnings**: dependency update suggestions, existing compatibility APIs/hooks and UI/layout suggestions. They are not all eliminated, and the project does not claim a warning-free build. Dependency migrations are kept separate from the first beta’s behavior changes.

## Device checks

The public source passed seven account-free QA checks on a physical Samsung Galaxy S21 Ultra through AndroidJUnitRunner. The follow-list integration test needs a signed-in test account and was excluded. The Gradle device task reported an aggregate failure despite its seven-test XML reporting zero failures; direct runner output independently confirmed `OK (7 tests)`. Unit, lint and release-build tasks passed normally.

The stable source preceding the public packaging also underwent a roughly two-minute large-category scroll that sampled 49 distinct displayed names without a sampled reappearance; upstream overlapping rows were dropped. Background/resume, refresh and selected-sort/language/tag resets were exercised.

These samples complement deterministic pagination tests. They do not prove that every possible live directory response or device is covered. The optimized public APK passed an in-place upgrade, cold launch, About/attribution and live playback smoke check. Its first-install identity and signing certificate were preserved.

## Short manual checklist

1. **Browse:** slowly scroll a large category. Creators should not repeat and visible rows should not reshuffle.
2. **Refresh:** refresh manually; a fresh snapshot and viewer counts should load.
3. **Query changes:** change sort, selected tag and language; each should start a new list while preserving the selected semantics.
4. **Rules:** add a temporary rule for a supplied tag, including one outside the visible chips. Inspect “Why blocked?” and remove the rule afterward.
5. **Accounts/proxies:** switch accounts; global rules and edited proxy order should stay. Select a preset/custom endpoint and verify playback/fallback behavior.
6. **Player:** play a live stream, test LIVE, volume/brightness swipes, minimize/restore and background/resume.
7. **Points:** load the balance/rewards. Claim or spend only when you intend to perform the actual Twitch action.

## Limits

- Twitch directory churn prevents a complete server-side snapshot; bounded recovery may stop early on an unstable or heavily filtered query.
- Tags and other filter metadata are only as complete as the chosen API response.
- Proxy uptime, a fixed live-delay guarantee, long battery endurance and all device/OEM combinations are not certified.
- Real point-spending transactions and actual push/notification delivery are not part of unattended tests.
- Internal Twitch APIs can change. A failed or ambiguous mutation is not automatically retried.
- No software release can promise zero bugs. Report reproducible issues with redacted diagnostics and the exact version.
