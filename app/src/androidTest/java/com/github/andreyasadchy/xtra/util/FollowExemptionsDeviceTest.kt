package com.github.andreyasadchy.xtra.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.andreyasadchy.xtra.XtraApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only test: never follows/unfollows channels or changes account credentials. */
@RunWith(AndroidJUnit4::class)
class FollowExemptionsDeviceTest {
    @Test fun existingTwitchFollowsAreAutomaticallyExempt() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as XtraApp
        val account = app.tokenPrefs().getString(C.USER_ID, null)
        assumeTrue(!account.isNullOrBlank())
        val response = app.xtraModule.graphQLRepository.loadQueryUserFollowedUsers(
            app.prefs().getString(C.NETWORK_LIBRARY, C.OKHTTP), TwitchApiHelper.getGQLHeaders(app,true), 1, null)
        assertTrue("Follow-list request failed", response.errors.isNullOrEmpty())
        val followed = response.data?.user?.follows?.edges?.firstOrNull()?.node?.id
        assumeTrue(followed != null)
        FollowExemptions.invalidate()
        assertTrue("An existing Twitch follow was missing from the exemptions", followed in FollowExemptions.current())
        assertEquals(account, app.tokenPrefs().getString(C.USER_ID,null))
    }
}
