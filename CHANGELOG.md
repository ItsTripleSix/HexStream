# Changelog

## 1.0.0-beta.1

First public HexStream beta, based on Xtra 2.60.7.

### Browsing and filters

- Append-only category snapshots with broadcaster/login/stream alias deduplication.
- Repeated-cursor and no-progress detection, bounded recovery, and internal aggregation of thin or duplicate-heavy pages.
- Separate 100-item upstream metadata batches and 30-row Paging pages; lazy thumbnails.
- Preserve visible ordering and captured viewer counts until refresh; reset on category, sort, selected-tag or language changes.
- Retain API fallback order without weakening sort/tag/language semantics.
- Native global channel/category/title/tag filters using the complete tag list supplied by each supported response.
- Exact/contains/regex rules, field/creator/word exceptions, active-account follow exemptions, optional language and emoji controls.
- Recent blocking explanations, card actions, and Unwanted Twitch/full-app JSON import/export.
- Empty editable default filter lists and opt-in language/emoji restrictions.
- Atomic compiled-filter cache and targeted list updates.

### Playback and accounts

- Six global ordered Luminous/PerfProd playback presets, with custom editing and player fallback retained.
- Saved-account switching with encrypted credentials and notifications only for the active account.
- Default low-delay live ExoPlayer tuning and compatible Media3 LoadControl forwarding.
- Optional brightness/volume swipe controls alongside existing minimize/miniplayer gestures.
- Bonus chest claims and channel reward redemption with active-account checks, confirmation and duplicate-submit protection.
- Lifecycle, player recovery, link validation, updater and accessibility improvements.

### Identity and distribution

- HexStream name and custom loop icon, including adaptive/monochrome variants.
- In-app About with Xtra credits and source links.
- HexStream-specific release feed, including beta releases.
- Locally built optimized APK, public corresponding source and documentation under AGPL v3.

See [verification](docs/VERIFICATION.md) for checks and limits. This is a beta; Twitch service/API changes and device-specific behavior remain possible.
