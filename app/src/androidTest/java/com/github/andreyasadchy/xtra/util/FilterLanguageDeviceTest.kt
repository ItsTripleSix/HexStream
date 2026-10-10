package com.github.andreyasadchy.xtra.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** No account, preference, database or notification mutations. Safe on the user's installation. */
@RunWith(AndroidJUnit4::class)
class FilterLanguageDeviceTest {
    @Test fun bundledModelDetectsTitlesWithoutCredentials() = runBlocking {
        assertEquals("en", ContentFilters.detectTitle("Today we are playing games together and having a great time"))
        assertEquals("es", ContentFilters.detectTitle("Hoy estamos jugando juntos y vamos a pasar un buen rato"))
        assertEquals("fr", ContentFilters.detectTitle("Aujourd'hui nous jouons ensemble et nous passons un bon moment"))
        assertEquals("de", ContentFilters.detectTitle("Heute spielen wir zusammen und haben eine gute Zeit"))
        assertNull(ContentFilters.detectTitle("hola"))
    }
}
