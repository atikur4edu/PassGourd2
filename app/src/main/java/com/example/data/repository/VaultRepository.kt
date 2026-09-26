package com.example.data.repository

import com.example.data.dao.VaultItemDao
import com.example.data.model.VaultCategory
import com.example.data.model.VaultItem
import com.example.security.CryptoEngine
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class VaultRepository(private val dao: VaultItemDao) {

    fun getAllItems(): Flow<List<VaultItem>> = dao.getAllItems()

    fun getItemsByCategory(category: String): Flow<List<VaultItem>> =
        dao.getItemsByCategory(category)

    fun getFavoriteItems(): Flow<List<VaultItem>> = dao.getFavoriteItems()

    fun searchItems(query: String): Flow<List<VaultItem>> = dao.searchItems(query)

    fun getItemById(id: Long): Flow<VaultItem?> = dao.getItemById(id)

    suspend fun getItemByIdDirect(id: Long): VaultItem? = dao.getItemByIdDirect(id)

    suspend fun getAllItemsDirect(): List<VaultItem> = dao.getAllItemsDirect()

    suspend fun findMatchingLogins(term: String): List<VaultItem> = dao.findMatchingLogins(term)

    suspend fun saveItem(
        id: Long = 0,
        title: String,
        username: String,
        plainPassword: String,
        websiteUrl: String,
        category: VaultCategory,
        notes: String = "",
        isFavorite: Boolean = false,
        requireBiometric: Boolean = false
    ): Long {
        val encryptedPassword = CryptoEngine.encrypt(plainPassword)
        val now = System.currentTimeMillis()

        val historyJson = if (id > 0) {
            val existing = dao.getItemByIdDirect(id)
            val existingDecrypted = existing?.let { CryptoEngine.decrypt(it.encryptedPassword) }
            if (existing != null && existingDecrypted != plainPassword && existingDecrypted?.isNotEmpty() == true) {
                appendPasswordHistory(existing.passwordHistoryJson, existingDecrypted, existing.updatedAt)
            } else {
                existing?.passwordHistoryJson ?: "[]"
            }
        } else {
            "[]"
        }

        val item = VaultItem(
            id = id,
            title = title.trim(),
            username = username.trim(),
            encryptedPassword = encryptedPassword,
            websiteUrl = websiteUrl.trim(),
            category = category.name,
            notes = notes.trim(),
            isFavorite = isFavorite,
            requireBiometric = requireBiometric,
            passwordHistoryJson = historyJson,
            updatedAt = now,
            createdAt = if (id > 0) (dao.getItemByIdDirect(id)?.createdAt ?: now) else now
        )

        return if (id == 0L) {
            dao.insert(item)
        } else {
            dao.update(item)
            id
        }
    }

    private fun appendPasswordHistory(historyJson: String, oldPassword: String, timestamp: Long): String {
        return try {
            val array = JSONArray(historyJson)
            val entry = JSONObject().apply {
                put("password", CryptoEngine.encrypt(oldPassword))
                put("timestamp", timestamp)
            }
            array.put(entry)
            array.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    fun getDecryptedPassword(item: VaultItem): String {
        return CryptoEngine.decrypt(item.encryptedPassword)
    }

    suspend fun deleteItem(id: Long) {
        dao.deleteById(id)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun populateDemoDataIfEmpty() {
        if (dao.getCount() == 0) {
            val demoList = listOf(
                VaultItem(
                    title = "Google Workspace",
                    username = "user.name@gmail.com",
                    encryptedPassword = CryptoEngine.encrypt("pA55w0rd!#Goog927"),
                    websiteUrl = "https://accounts.google.com",
                    category = VaultCategory.LOGIN.name,
                    notes = "Primary personal and cloud account with 2FA enabled",
                    isFavorite = true,
                    requireBiometric = false
                ),
                VaultItem(
                    title = "GitHub",
                    username = "octocat_developer",
                    encryptedPassword = CryptoEngine.encrypt("ghp_K9#xL01pQ99@securePass!"),
                    websiteUrl = "https://github.com",
                    category = VaultCategory.LOGIN.name,
                    notes = "Personal SSH keys and repository access",
                    isFavorite = true,
                    requireBiometric = true
                ),
                VaultItem(
                    title = "Amazon Shopping",
                    username = "user.name@gmail.com",
                    encryptedPassword = CryptoEngine.encrypt("AmznShopper$2026!"),
                    websiteUrl = "https://amazon.com",
                    category = VaultCategory.LOGIN.name,
                    notes = "Prime membership active",
                    isFavorite = false,
                    requireBiometric = false
                ),
                VaultItem(
                    title = "Apex National Bank",
                    username = "apex_client_8492",
                    encryptedPassword = CryptoEngine.encrypt("Vault#Bank$9914*Key"),
                    websiteUrl = "https://online.apexbank.com",
                    category = VaultCategory.CARD.name,
                    notes = "Account ending in 4102. Requires biometric verification for reveals.",
                    isFavorite = true,
                    requireBiometric = true
                ),
                VaultItem(
                    title = "Home Office Wi-Fi",
                    username = "PassGuard-Secure-5G",
                    encryptedPassword = CryptoEngine.encrypt("WPA3-FiberOptic-9872!#"),
                    websiteUrl = "",
                    category = VaultCategory.NOTE.name,
                    notes = "Router admin portal: 192.168.1.1 (admin/FiberOptic9872)",
                    isFavorite = false,
                    requireBiometric = false
                ),
                VaultItem(
                    title = "Cloud Infrastructure API",
                    username = "svc-passguard-prod",
                    encryptedPassword = CryptoEngine.encrypt("sk_live_94827103847192837192"),
                    websiteUrl = "https://cloud.google.com",
                    category = VaultCategory.SECURE_KEY.name,
                    notes = "Production service account bearer credentials",
                    isFavorite = false,
                    requireBiometric = true
                )
            )
            dao.insertAll(demoList)
        }
    }
}
