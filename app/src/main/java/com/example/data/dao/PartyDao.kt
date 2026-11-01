package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Party
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties ORDER BY updatedAt DESC")
    fun getAllParties(): Flow<List<Party>>

    @Query("SELECT * FROM parties ORDER BY updatedAt DESC")
    suspend fun getAllPartiesOnce(): List<Party>

    @Query("SELECT * FROM parties WHERE id = :partyId LIMIT 1")
    fun getPartyById(partyId: Long): Flow<Party?>

    @Query("SELECT * FROM parties WHERE id = :partyId LIMIT 1")
    suspend fun getPartyByIdOnce(partyId: Long): Party?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: Party): Long

    @Update
    suspend fun updateParty(party: Party)

    @Delete
    suspend fun deleteParty(party: Party)

    @Query("DELETE FROM parties")
    suspend fun deleteAllParties()
}
