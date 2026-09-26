package com.example.autofill

import android.app.assist.AssistStructure
import android.content.Context
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.example.R
import com.example.data.database.PassGuardDatabase
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.security.CryptoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PassGuardAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class ParsedForm(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var webDomain: String? = null,
        var packageName: String? = null,
        var extractedUsernameVal: String? = null,
        var extractedPasswordVal: String? = null
    )

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val parsed = parseStructure(structure)
                val usernameId = parsed.usernameId
                val passwordId = parsed.passwordId

                if (usernameId == null && passwordId == null) {
                    callback.onSuccess(null)
                    return@launch
                }

                val db = PassGuardDatabase.getInstance(applicationContext)
                val allLogins = db.vaultItemDao().getAllItemsDirect()

                // Filter matching candidates
                val domain = parsed.webDomain?.lowercase() ?: ""
                val pkg = parsed.packageName?.lowercase() ?: ""

                val matches = allLogins.filter { item ->
                    val url = item.websiteUrl.lowercase()
                    val title = item.title.lowercase()
                    (domain.isNotEmpty() && (url.contains(domain) || domain.contains(url) || title.contains(domain))) ||
                    (pkg.isNotEmpty() && (url.contains(pkg) || pkg.contains(title)))
                }.ifEmpty {
                    // Fallback to all login items or top 5 logins
                    allLogins.take(5)
                }

                if (matches.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()

                for (item in matches) {
                    val decryptedPassword = CryptoEngine.decrypt(item.encryptedPassword)
                    val presentation = RemoteViews(packageName, R.layout.autofill_dataset_item).apply {
                        setTextViewText(R.id.autofill_title, item.title)
                        setTextViewText(
                            R.id.autofill_subtitle,
                            if (item.username.isNotEmpty()) item.username else "Tap to fill"
                        )
                    }

                    val datasetBuilder = Dataset.Builder(presentation)
                    var hasValue = false

                    if (usernameId != null && item.username.isNotEmpty()) {
                        datasetBuilder.setValue(usernameId, AutofillValue.forText(item.username))
                        hasValue = true
                    }
                    if (passwordId != null && decryptedPassword.isNotEmpty()) {
                        datasetBuilder.setValue(passwordId, AutofillValue.forText(decryptedPassword))
                        hasValue = true
                    }

                    if (hasValue) {
                        responseBuilder.addDataset(datasetBuilder.build())
                    }
                }

                // Add SaveInfo so user can save passwords entered in other apps
                val requiredIds = mutableListOf<AutofillId>()
                if (passwordId != null) requiredIds.add(passwordId)
                if (usernameId != null) requiredIds.add(usernameId)

                if (requiredIds.isNotEmpty()) {
                    val saveInfo = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME,
                        requiredIds.toTypedArray()
                    ).build()
                    responseBuilder.setSaveInfo(saveInfo)
                }

                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess()
            return
        }

        serviceScope.launch {
            try {
                val parsed = parseStructure(structure)
                val username = parsed.extractedUsernameVal ?: ""
                val password = parsed.extractedPasswordVal ?: ""
                val domain = parsed.webDomain ?: parsed.packageName ?: "Saved Login"

                if (password.isNotEmpty()) {
                    val db = PassGuardDatabase.getInstance(applicationContext)
                    val title = domain.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                        .split("/").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Saved Login"

                    val newItem = VaultItem(
                        title = title,
                        username = username,
                        encryptedPassword = CryptoEngine.encrypt(password),
                        websiteUrl = parsed.webDomain ?: "",
                        category = VaultCategory.LOGIN.name,
                        notes = "Auto-saved by PassGuard Autofill Service",
                        isFavorite = false,
                        requireBiometric = false
                    )
                    db.vaultItemDao().insert(newItem)
                }
                callback.onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onSuccess()
            }
        }
    }

    private fun parseStructure(structure: AssistStructure): ParsedForm {
        val form = ParsedForm()
        val windowNodes = (0 until structure.windowNodeCount).map { structure.getWindowNodeAt(it) }
        for (window in windowNodes) {
            val root = window.rootViewNode
            form.packageName = structure.activityComponent?.packageName
            traverseNode(root, form)
        }
        return form
    }

    private fun traverseNode(node: AssistStructure.ViewNode, form: ParsedForm) {
        node.webDomain?.let {
            if (form.webDomain == null) form.webDomain = it
        }

        val hints = node.autofillHints?.map { it.lowercase() } ?: emptyList()
        val hintText = (node.hint ?: "").lowercase()
        val text = node.text?.toString() ?: ""
        val inputType = node.inputType
        val isPasswordType = (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        val isEmailType = (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS

        val isUsernameHint = hints.contains(View.AUTOFILL_HINT_USERNAME) ||
                hints.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS) ||
                hintText.contains("username") || hintText.contains("email") || hintText.contains("user id")

        val isPasswordHint = hints.contains(View.AUTOFILL_HINT_PASSWORD) ||
                hints.contains("new_password") ||
                hintText.contains("password") || hintText.contains("passcode") || hintText.contains("pin")

        if (form.passwordId == null && (isPasswordType || isPasswordHint)) {
            form.passwordId = node.autofillId
            if (text.isNotEmpty()) form.extractedPasswordVal = text
        } else if (form.usernameId == null && (isEmailType || isUsernameHint)) {
            form.usernameId = node.autofillId
            if (text.isNotEmpty()) form.extractedUsernameVal = text
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), form)
        }
    }
}
