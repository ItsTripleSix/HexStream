package com.github.andreyasadchy.xtra.util

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.github.andreyasadchy.xtra.XtraModule
import com.github.andreyasadchy.xtra.model.NotificationUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AccountDatabaseDeviceTest {
    @Test fun notificationSelectionsAndHistoryBelongToOnlyOneAccount() = runBlocking {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("Run fixture tests only in the isolated QA app", "com.github.andreyasadchy.xtra.qa", c.packageName)
        assertTrue("Use a clean prototype installation", PrototypeAccounts.accounts(c).isEmpty())
        val previous = c.tokenPrefs().getString(C.USER_ID, null)
        val names = listOf("account_990000003", "account_990000004")
        assertFalse(c.databaseList().any { it in names })
        var a: XtraModule? = null
        var b: XtraModule? = null
        try {
            c.tokenPrefs().edit().putString(C.USER_ID, "990000003").commit()
            a = XtraModule(c.applicationContext as Application)
            a.notificationsRepository.saveUser(NotificationUser("dummy_channel"))
            c.tokenPrefs().edit().putString(C.USER_ID, "990000004").commit()
            b = XtraModule(c.applicationContext as Application)
            assertNull(b.notificationsRepository.getUserById("dummy_channel"))
            assertNotNull(a.notificationsRepository.getUserById("dummy_channel"))
            assertNotEquals(a.database.openHelper.databaseName, b.database.openHelper.databaseName)
        } finally {
            a?.database?.close()
            b?.database?.close()
            c.tokenPrefs().edit().putString(C.USER_ID, previous).commit()
            names.forEach { c.deleteDatabase(it) }
        }
    }
}
