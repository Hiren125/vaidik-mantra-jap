package com.vedicapps.mantrajap

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MantraDao {

    // 1. For the Dashboard: Updates automatically when data changes
    @Query("SELECT * FROM mantras ORDER BY isFixed DESC, id ASC")
    fun getAllMantras(): Flow<List<Mantra>>

    // 2. For the Player: Stays updated if the count changes while you look at it
    @Query("SELECT * FROM mantras WHERE id = :id")
    fun getMantraById(id: Long): Flow<Mantra?>

    // 3. THE SYNC FETCH: Fetches the data once without a "Flow"
    // Use this inside a coroutine when you just need the data right now.
    @Query("SELECT * FROM mantras WHERE id = :id")
    suspend fun getMantraSync(id: Long): Mantra?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMantra(mantra: Mantra)

    @Update
    suspend fun updateMantra(mantra: Mantra)

    @Delete
    suspend fun deleteMantra(mantra: Mantra)

    // Wipes suggested mantras so we can refresh from Firebase
    @Query("DELETE FROM mantras WHERE isFixed = 1")
    suspend fun deleteFixedMantras()

    // Fast update for the counter
    @Query("UPDATE mantras SET count = :newCount WHERE id = :id")
    suspend fun updateCount(id: Long, newCount: Int)

    // Resets the counter
    @Query("UPDATE mantras SET count = 0 WHERE id = :id")
    suspend fun resetCount(id: Long)

    // Finds a mantra by its Firebase key
    @Query("SELECT * FROM mantras WHERE remoteKey = :key LIMIT 1")
    suspend fun getMantraByRemoteKey(key: String): Mantra?
}