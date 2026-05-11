package io.github.visiongem.ledger.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.visiongem.ledger.core.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Upsert
    suspend fun upsert(account: AccountEntity): Long

    @Query("DELETE FROM account WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM account ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM account")
    suspend fun getAll(): List<AccountEntity>
}
