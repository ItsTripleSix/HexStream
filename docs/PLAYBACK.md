# Playback, proxies, accounts and points

## Global playback presets

The custom playback proxy list is stored once in a global database. It is seeded only when the global list is first created and empty. Existing account-specific custom URLs are imported once without deleting account data or overwriting the global list. Switching accounts does not reseed, reorder or clear it.

| Initial order | Preset | URL |
| --- | --- | --- |
| 1 | EU2 Luminous | `https://eu2.luminous.dev/live/$channel?allow_source=true&allow_audio_only=true&fast_bread=true` |
| 2 | EU Luminous | `https://eu.luminous.dev/live/$channel?allow_source=true&allow_audio_only=true&fast_bread=true` |
| 3 | PerfProd North America | `https://lb-na.cdn-perfprod.com/live/$channel?allow_source=true&allow_audio_only=true&fast_bread=true` |
| 4 | PerfProd Europe | `https://lb-eu.cdn-perfprod.com/live/$channel?allow_source=true&allow_audio_only=true&fast_bread=true` |
| 5 | PerfProd Europe 2 | `https://lb-eu2.cdn-perfprod.com/live/$channel?allow_source=true&allow_audio_only=true&fast_bread=true` |
| 6 | PerfProd Asia | `https://lb-as.cdn-perfprod.com/live/$channel?allow_source=true&allow_audio_only=true&fast_bread=true` |

`$channel` is replaced with the stream’s channel login. Edit, reorder, enable/disable or add custom entries through the existing playback proxy settings. Deleting a preset is respected; account switching does not restore it.

The existing player opens enabled custom endpoints in configured order. A working endpoint is retained for the current playback session. Supported failures can move to the next entry; in particular, an HTTP 404 on a custom endpoint no longer immediately treats the stream as unavailable. Other player retries and direct paths retain their existing behavior. This is not a promise that every proxy failure will be detected before playback starts or that the lowest-latency region will be chosen automatically.

Proxy settings and pagination have separate implementations and state. Independent operators control availability, content and delay. A proxy necessarily receives playback requests and network information; review [privacy](PRIVACY.md) before adding an endpoint you do not trust.

## Live delay

Low-delay mode is enabled by default for live ExoPlayer playback. The target live offset is **1,000 ms**, with bounded playback-speed adjustment from **0.97× to 1.08×**. Live startup/rebuffer thresholds are **500 ms / 1,000 ms**; player-speed changes are accounted for. VOD and disabled low-delay paths keep their normal buffering behavior.

This target measures the player’s position relative to the stream’s available live edge. It does not mean that the phone is guaranteed one second behind the broadcaster or another device. Twitch/proxy playlist generation, stream encoding, network conditions and buffering all contribute to end-to-end delay. Tap LIVE to return to the current edge if playback falls behind; turn off low-delay mode if your connection needs more buffering.

The custom `LiveLoadControl` explicitly forwards the Media3 `LoadControl` contract, including Java default methods. A compatibility test checks delegation when the Media3 API changes.

## Gestures

With the player controls hidden, vertical swipes in the left quarter adjust window brightness; swipes in the right quarter adjust media volume. Each control can be enabled separately. A 24 dp exclusion near the system edges and multi-touch handling reduce conflicts with navigation.

The existing center drag/minimize and miniplayer interactions remain available. Temporary window brightness is restored when leaving the player. Gestures operate on playback UI state and do not affect filters or account selection.

## Saved accounts and notifications

Multiple Twitch accounts can be saved, with one active at a time. Saved credentials use an Android Keystore-backed AES-GCM vault. Account databases/settings remain separate from the global content-filter and proxy stores.

The followed-channel exception uses follows belonging to the current active account, including available locally followed creators. Switching accounts updates that context; it does not merge all saved accounts’ follows.

The background notification worker checks the active account before processing, before individual notifications and before the summary. There is no cross-account aggregate notification mode. System notification permissions and Twitch/network availability still determine whether a notification can be delivered.

## Bonus chest and channel rewards

The Points panel can show the current balance, an available periodic bonus claim and the stream’s custom channel rewards. It checks the active account and current channel before making a request. Reward rows show their cost/availability; a reward requiring text input collects it before final confirmation.

Claiming a chest and spending points are different actions. Spending requires an explicit confirmation. A submission gate prevents repeated taps or a second confirmation from creating duplicate requests. Mutating operations are not automatically retried on an ambiguous failure; success requires a confirmed response, including a redemption identity for rewards. Read-only balance/reward loads can be retried.

Xtra’s existing automatic bonus preference remains available. Reward availability and Twitch’s internal GraphQL behavior can change. Real point spending is not part of unattended regression testing.
