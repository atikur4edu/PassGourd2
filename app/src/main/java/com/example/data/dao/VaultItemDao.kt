package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VaultItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE category = :category ORDER BY isFavorite DESC, updatedAt DESC")
    fun getItemsByCategory(category: String): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR websiteUrl LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchItems(query: String): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    fun getItemById(id: Long): Flow<VaultItem?>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdDirect(id: Long): VaultItem?

    @Query("SELECT * FROM vault_items ORDER BY updatedAt DESC")
    suspend fun getAllItemsDirect(): List<VaultItem>

    @Query("SELECT * FROM vault_items WHERE category = 'LOGIN' AND (title LIKE '%' || :term || '%' OR websiteUrl LIKE '%' || :term || '%') ORDER BY updatedAt DESC LIMIT 10")
    suspend fun findMatchingLogins(term: String): List<VaultItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VaultItem>)

    @Update
    suspend fun update(item: VaultItem)

    @Delete
    suspend fun delete(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE vault_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM vault_items")
    suspend fun getCount(): Int
}
