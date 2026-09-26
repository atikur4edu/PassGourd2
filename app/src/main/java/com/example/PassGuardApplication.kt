package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.data.database.PassGuardDatabase
import com.example.data.repository.VaultRepository
import com.example.security.VaultLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PassGuardApplication : Application(), Application.ActivityLifecycleCallbacks {

    val database by lazy { PassGuardDatabase.getInstance(this) }
    val repository by lazy { VaultRepository(database.vaultItemDao()) }
    val lockManager by lazy { VaultLockManager(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        // Seed demo data on initial app run
        applicationScope.launch {
            repository.populateDemoDataIfEmpty()
        }
    }

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (activityCount == 1) {
            lockManager.onAppForegrounded()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            lockManager.onAppBackgrounded()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
