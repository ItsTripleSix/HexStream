package com.github.andreyasadchy.xtra.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchLinkPolicyTest {
    @Test fun acceptsNormalChannelsVideosAndClips() {
        listOf("https://www.twitch.tv/triplesix", "https://twitch.tv/videos/123?t=1m",
            "https://clips.twitch.tv/Example", "http://m.twitch.tv/test", "https://TWITCH.TV:443/test")
            .forEach { assertTrue(it, TwitchLinkPolicy.isTrusted(it)) }
    }

    @Test fun rejectsDomainSpoofsAndCredentials() {
        listOf("https://twitch.tv.evil.example/test", "https://eviltwitch.tv/test",
            "https://evil.example/?next=https://twitch.tv/test", "https://twitch.tv@evil.example/test",
            "https://evil.example@twitch.tv/test", "https://twitch.tv:8080/test")
            .forEach { assertFalse(it, TwitchLinkPolicy.isTrusted(it)) }
    }

    @Test fun rejectsUnsupportedSchemesAndMalformedLinks() {
        listOf("javascript:alert(1)", "file:///twitch.tv/test", "intent://twitch.tv/test",
            "//twitch.tv/test", "https://", "https://twitch.tv/%broken", "")
            .forEach { assertFalse(it, TwitchLinkPolicy.isTrusted(it)) }
    }
}
