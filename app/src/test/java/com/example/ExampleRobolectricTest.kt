package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.security.VaultLockManager
import com.example.util.PasswordGenerator
import com.example.util.SecurityAuditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PassGuard", appName)
    }

    @Test
    fun `vault lock manager verifies default pin and updates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lockManager = VaultLockManager(context)

        // Default PIN is 1234
        assertTrue(lockManager.verifyPin("1234"))
        assertFalse(lockManager.verifyPin("9999"))

        // Update PIN
        lockManager.setMasterPin("4321")
        assertTrue(lockManager.verifyPin("4321"))
        assertFalse(lockManager.verifyPin("1234"))
    }

    @Test
    fun `security auditor flags weak and reused passwords`() {
        val items = listOf(
            VaultItem(
                id = 1,
                title = "Account A",
                username = "user1",
                encryptedPassword = "RAW:" + android.util.Base64.encodeToString("weak1".toByteArray(), android.util.Base64.NO_WRAP),
                category = VaultCategory.LOGIN.name
            ),
            VaultItem(
                id = 2,
                title = "Account B",
                username = "user2",
                encryptedPassword = "RAW:" + android.util.Base64.encodeToString("weak1".toByteArray(), android.util.Base64.NO_WRAP),
                category = VaultCategory.LOGIN.name
            ),
            VaultItem(
                id = 3,
                title = "Account C",
                username = "user3",
                encryptedPassword = "RAW:" + android.util.Base64.encodeToString("Str0ng!#Pass_9271K@".toByteArray(), android.util.Base64.NO_WRAP),
                category = VaultCategory.LOGIN.name
            )
        )

        val report = SecurityAuditor.generateReport(items)
        assertEquals(3, report.totalCount)
        assertTrue("Should detect weak passwords", report.weakCount >= 2)
        assertTrue("Should detect reused passwords", report.reusedCount >= 2)
    }
}
