package com.example.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.autofill.AutofillHelper
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.data.repository.VaultRepository
import com.example.security.BiometricAuthManager
import com.example.security.BiometricStatus
import com.example.security.VaultLockManager
import com.example.util.AuditReport
import com.example.util.SecurityAuditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VaultTab(val label: String) {
    VAULT("Vault"),
    GENERATOR("Generator"),
    AUDIT("Security Audit"),
    AUTOFILL("Autofill"),
    SETTINGS("Settings")
}

class VaultViewModel(
    private val repository: VaultRepository,
    private val lockManager: VaultLockManager
) : ViewModel() {

    val isVaultLocked: StateFlow<Boolean> = lockManager.isLocked

    private val _activeTab = MutableStateFlow(VaultTab.VAULT)
    val activeTab: StateFlow<VaultTab> = _activeTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _revealedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val revealedItemIds: StateFlow<Set<Long>> = _revealedItemIds.asStateFlow()

    private val _isAutofillEnabled = MutableStateFlow(false)
    val isAutofillEnabled: StateFlow<Boolean> = _isAutofillEnabled.asStateFlow()

    private val _biometricStatus = MutableStateFlow(BiometricStatus.UNKNOWN)
    val biometricStatus: StateFlow<BiometricStatus> = _biometricStatus.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(lockManager.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _autoLockTimeoutSec = MutableStateFlow(lockManager.getAutoLockTimeoutSeconds())
    val autoLockTimeoutSec: StateFlow<Int> = _autoLockTimeoutSec.asStateFlow()

    val allVaultItems: StateFlow<List<VaultItem>> = repository.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredItems: StateFlow<List<VaultItem>> = combine(
        allVaultItems,
        _searchQuery,
        _selectedCategory
    ) { items, query, category ->
        var result = items
        if (category != null) {
            result = if (category == "FAVORITES") {
                result.filter { it.isFavorite }
            } else {
                result.filter { it.category == category }
            }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.username.lowercase().contains(q) ||
                it.websiteUrl.lowercase().contains(q) ||
                it.notes.lowercase().contains(q)
            }
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val auditReport: StateFlow<AuditReport> = allVaultItems.combine(_activeTab) { items, _ ->
        SecurityAuditor.generateReport(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuditReport(0, 0, 0, 0, 100, emptyList())
    )

    fun setActiveTab(tab: VaultTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(id: Long, currentVal: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(id, !currentVal)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteItem(id)
        }
    }

    fun getDecryptedPassword(item: VaultItem): String {
        return repository.getDecryptedPassword(item)
    }

    fun saveVaultItem(
        id: Long = 0,
        title: String,
        username: String,
        plainPassword: String,
        websiteUrl: String,
        category: VaultCategory,
        notes: String,
        isFavorite: Boolean,
        requireBiometric: Boolean,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.saveItem(
                id = id,
                title = title,
                username = username,
                plainPassword = plainPassword,
                websiteUrl = websiteUrl,
                category = category,
                notes = notes,
                isFavorite = isFavorite,
                requireBiometric = requireBiometric
            )
            launch(Dispatchers.Main) {
                onComplete(newId)
            }
        }
    }

    fun toggleRevealPassword(item: VaultItem, activity: FragmentActivity) {
        val currentSet = _revealedItemIds.value
        if (currentSet.contains(item.id)) {
            _revealedItemIds.value = currentSet - item.id
        } else {
            if (item.requireBiometric && _isBiometricEnabled.value && BiometricAuthManager.isBiometricAvailable(activity)) {
                BiometricAuthManager.promptBiometric(
                    activity = activity,
                    title = "Reveal Password",
                    subtitle = "Authenticate to view credentials for ${item.title}",
                    onSuccess = {
                        _revealedItemIds.value = _revealedItemIds.value + item.id
                    },
                    onError = {
                        Toast.makeText(activity, "Biometric check failed: $it", Toast.LENGTH_SHORT).show()
                    },
                    onNegativeButton = {
                        // Allow fallback to PIN or reveal
                        _revealedItemIds.value = _revealedItemIds.value + item.id
                    }
                )
            } else {
                _revealedItemIds.value = currentSet + item.id
            }
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String, isSensitive: Boolean) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun lockVault() {
        lockManager.lock()
        _revealedItemIds.value = emptySet()
    }

    fun unlockWithPin(pin: String): Boolean {
        return if (lockManager.verifyPin(pin)) {
            lockManager.unlock()
            true
        } else {
            false
        }
    }

    fun unlockWithBiometrics(activity: FragmentActivity, onComplete: (Boolean) -> Unit) {
        if (!lockManager.isBiometricEnabled()) {
            onComplete(false)
            return
        }

        BiometricAuthManager.promptBiometric(
            activity = activity,
            title = "Unlock PassGuard",
            subtitle = "Scan your fingerprint or face to access your vault",
            onSuccess = {
                lockManager.unlock()
                onComplete(true)
            },
            onError = {
                onComplete(false)
            },
            onNegativeButton = {
                onComplete(false)
            }
        )
    }

    fun refreshSystemStatus(context: Context) {
        _isAutofillEnabled.value = AutofillHelper.isPassGuardAutofillEnabled(context)
        _biometricStatus.value = BiometricAuthManager.getBiometricStatus(context)
    }

    fun setBiometricSetting(enabled: Boolean) {
        lockManager.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
    }

    fun setAutoLockTimeout(seconds: Int) {
        lockManager.setAutoLockTimeoutSeconds(seconds)
        _autoLockTimeoutSec.value = seconds
    }

    fun changeMasterPin(oldPin: String, newPin: String): Boolean {
        return if (lockManager.verifyPin(oldPin)) {
            lockManager.setMasterPin(newPin)
            true
        } else {
            false
        }
    }

    fun resetDemoVault() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.populateDemoDataIfEmpty()
        }
    }

    class Factory(
        private val repository: VaultRepository,
        private val lockManager: VaultLockManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VaultViewModel(repository, lockManager) as T
        }
    }
}
