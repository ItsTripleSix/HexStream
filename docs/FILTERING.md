# Tags and content filtering

## Defaults and scope

All editable block and allow lists start empty. Language restrictions and emoji blocking start off. The filter engine is available immediately, but it hides nothing until you configure rules. Saved settings are not replaced on upgrade.

Filters are global across saved accounts. Account switching changes the active account and its followed-channel exemptions; it does not clear or reseed your rules. The main implementation is [`ContentFilters.kt`](../app/src/main/java/com/github/andreyasadchy/xtra/util/ContentFilters.kt), [`FilterRules.kt`](../app/src/main/java/com/github/andreyasadchy/xtra/util/FilterRules.kt) and the filter settings screens.

## What “all tags” means

Stream responses can contain more tags than a card can display. HexStream carries the whole supplied list into the filter engine. A tag does not need to be a visible chip to match a rule. Category GraphQL, persisted GraphQL and Helix stream paths retain all tag names provided by their respective responses.

Coverage depends on the response type:

| Item | Metadata considered when present |
| --- | --- |
| Live stream / channel | Login, display name, title, category, supplied tags and declared language. |
| Category card | Category name and supplied category tags. |
| Video / clip | Creator identity, title and category; tags/language are not inferred when absent. |

The engine does not crawl a complete Twitch tag catalog, guess omitted tags, or inspect stream audio/video to infer a tag. API response changes can affect available metadata.

In a category browsing session, filtering occurs before unique-row acceptance. If rules hide most of a batch, bounded pagination recovery looks for further visible unique rows. Changing a category’s selected Twitch tag/language/sort creates a fresh snapshot; editing global rules refreshes browsing when the filter screen returns its change result.

## Matching syntax

Channel, category, tag and title lists are separate. A channel rule matches the login or display name; a tag rule checks every supplied tag. Matching one field does not require a match in another field.

| Syntax | Meaning | Example |
| --- | --- | --- |
| Plain text | Normalized whole-field exact match | `MyTeam` matches the fictional tag `myteam`. |
| `~text` | Normalized contains match | `~myteam` matches `MyTeamIsPerfect`. |
| `*text*` | Contains convenience syntax | `*myteam*` behaves as contains. |
| `/pattern/i` | Regex search | `/^myteam/i` matches fictional tags beginning with MyTeam. |
| `'ExactName'` | Literal case-sensitive whole-field match | `'MyTeam'` does not match `myteam`. |

Normalization trims, uses Unicode NFKD, removes combining marks and lowercases with `Locale.ROOT`. Thus an ordinary exact rule `cafe` can match `Café`. Literal quoted rules bypass that normalization. Regex matching uses Java `Pattern.find`; functional flags include `i`, `m` and `s`. Do not assume every JavaScript regex feature or flag has identical behavior on Android.

Regexes and list entries are validated before replacing working settings. Individual rules are limited to 4,096 characters; imports are limited to 2,000,000 characters and 10,000 combined entries. Invalid imports leave the current working configuration in place.

## Exceptions and precedence

| Control | Scope |
| --- | --- |
| Disable filtering | Stops content filtering. |
| Allowed channels | Exempts the entire matching creator from these checks. |
| Allow followed channels | Exempts creators followed by the active account, including available local follows; enabled by default. |
| Allowed usernames | Exempts only a matching login/display-name field. |
| Allowed titles | Exempts the matching title, including title-language checks. |
| Allowed tags | Exempts the matching tag, not other tags or the title. |
| Allowed words | Protects complete matching Unicode word tokens inside fields. |
| Allowed emojis | Protects that complete emoji sequence; unrelated text and other emoji remain checked. |

Whole-creator exceptions win over block rules. Follow exemptions do not exempt an unrelated category card and are derived for the active account, not copied into the manual allow list.

Allowed words contain single words, not contains/regex rules or phrases. They protect only complete tokens from custom word matches, character checks and title-language evidence. For example, adding `Landmark` can protect that token from `~mark`, while `bookmark` and `landmarkgamer` can still match. They do not exempt an entire title, a language tag, a category rule, or the separate declared-language setting. The word list starts empty.

## Optional language controls

These controls are off on fresh installs and can be enabled independently as described by their settings:

- **English only:** recognizes a built-in set of non-English language tags. It is not a general spoken-language detector.
- **Title-language detection:** when enabled with English only, runs ML Kit’s on-device text language identifier on eligible titles. It uses a 90% confidence threshold, requires at least 20 letters, caps input at 1,000 characters, bounds work to 1.5 seconds and caches up to 512 title results. Decorations and protected words are removed from detection input; an uncertain result may retry without a leading camel-case handle. Unknown results are not treated as a foreign language.
- **Declared Twitch language alone:** allows the supplied language code to block even without another language signal. It is a separate opt-in under English only.
- **Non-English letters and accents:** checks non-ASCII letters/combining marks in login, display name, title and supplied tags after compatibility normalization. This can hide an English title containing an accented name. It is a character rule, not a reliable classification of a person’s language. Ordinary punctuation, numbers and emoji are handled separately.

The app does not listen to audio, transcribe speech, or promise perfect language inference. Narrow word/field exceptions can reduce false positives. Category names are governed by category rules, not the character control.

## Emoji controls

Default emoji blocking is off. Specific blocked and allowed lists let you choose individual sequences. If block-all is enabled, allowed sequences remain permitted. A specific allow entry takes precedence; saving an emoji to one list removes the same normalized sequence from the opposite list.

The tokenizer uses Unicode Emoji 17 sequences, including flags, skin tones and joined emoji. One complete sequence goes in each entry. `👍` and `👍🏽` are different choices; a heart with or without its presentation selector is treated as the same choice. Plain digits and ordinary punctuation are not emoji sequences. Some emoji-capable symbols, such as ©, are included.

Emoji-only rules from compatible blocking lists migrate into the emoji list. Other rules remain independent. Allowing one emoji does not cancel a separate title rule or language restriction. Unicode data licensing is preserved in the [asset notice](../app/src/main/assets/UNICODE-LICENSE.txt).

## “Why blocked?” and quick rules

The app retains up to 100 recent hidden item records in memory, with distinct rule, language/character and emoji causes. Each record keeps the matched field, rule and relevant evidence. Use search and tabs to inspect a cause, edit a matching rule or add a narrow exception. This list is temporary diagnostic state, not a persistent activity history.

Long-press supported normal or compact stream cards to act on a channel, title, individual tag or emoji. Changes require confirmation and apply globally. If a followed-channel or whole-creator exception still applies, that exemption wins; remove it or disable the followed-channel exception if you intend to hide that creator.

## Import and export

HexStream accepts the blocking-list JSON format used by [Unwanted Twitch](https://github.com/kwaschny/unwanted-twitch). Its `categories`, `channels`, `tags` and `titles` arrays map to the corresponding rule lists. A basic import does not silently enable language restrictions.

Want the same list-based control on desktop? [HexStream Filter for Firefox and Chrome](https://github.com/ItsTripleSix/HexStream-Filter) checks complete supplied tags and supports explicit compatible-list import/export. It is a separate browser extension: no automatic sync, and phone-only language/emoji/followed-account options do not transfer as browser features. [Installation](https://github.com/ItsTripleSix/HexStream-Filter/blob/main/docs/INSTALLING.md) · [Cross-platform limits](https://github.com/ItsTripleSix/HexStream-Filter/blob/main/docs/FILTERING.md#using-it-with-hexstream-for-android).

- **Full app export** preserves all filter options, allow lists and emoji choices for a HexStream round trip.
- **Export for Unwanted Twitch** emits compatible blocking lists. Specific blocked emojis are emitted as ordinary rules. That format cannot express HexStream’s allow lists, block-all emoji setting, followed-account exemptions or language controls.

Exports may reveal your preferences and channel choices. Store or share them only as you intend; exports are not required for ordinary bug reports.

## Reliability details

Compiled rules are cached together with the exact rules object as one atomic entry. Concurrent requests cannot obtain a rule set paired with another set’s compiled matcher. Bounded normalization/title caches avoid unlimited growth. The filter engine is separate from playback/proxy code, while category pagination consumes its filtered rows before deduplication.
