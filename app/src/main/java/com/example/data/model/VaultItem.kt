package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VaultCategory(val displayName: String) {
    LOGIN("Login"),
    CARD("Credit Card"),
    NOTE("Secure Note"),
    IDENTITY("Identity"),
    SECURE_KEY("API / 2FA Key")
}

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val username: String = "",
    val encryptedPassword: String = "",
    val websiteUrl: String = "",
    val category: String = VaultCategory.LOGIN.name,
    val notes: String = "",
    val isFavorite: Boolean = false,
    val requireBiometric: Boolean = false,
    val passwordHistoryJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
