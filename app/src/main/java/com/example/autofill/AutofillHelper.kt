package com.example.autofill

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager

object AutofillHelper {

    fun isAutofillSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    fun isPassGuardAutofillEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val autofillManager = context.getSystemService(AutofillManager::class.java)
            autofillManager?.hasEnabledAutofillServices() == true
        } else {
            false
        }
    }

    fun openAutofillSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general settings
                try {
                    val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                    context.startActivity(fallbackIntent)
                } catch (ignored: Exception) {}
            }
        }
    }
}
