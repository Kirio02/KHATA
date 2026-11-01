package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntry>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntry>

    @Query("SELECT * FROM transactions WHERE partyId = :partyId ORDER BY timestamp DESC")
    fun getTransactionsForParty(partyId: Long): Flow<List<TransactionEntry>>

    @Query("SELECT * FROM transactions WHERE partyId = :partyId ORDER BY timestamp DESC")
    suspend fun getTransactionsForPartyOnce(partyId: Long): List<TransactionEntry>

    @Query("SELECT * FROM transactions WHERE type IN ('CASH_IN', 'CASH_OUT') ORDER BY timestamp DESC")
    fun getCashbookTransactions(): Flow<List<TransactionEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entry: TransactionEntry): Long

    @Update
    suspend fun updateTransaction(entry: TransactionEntry)

    @Delete
    suspend fun deleteTransaction(entry: TransactionEntry)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
