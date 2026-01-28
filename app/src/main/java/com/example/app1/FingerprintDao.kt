package com.example.app1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface FingerprintDao {

    @Insert
    suspend fun insert(fp: Fingerprint)

    @Query("SELECT * FROM fingerprints")
    suspend fun getAll(): List<Fingerprint>

    @Update
    suspend fun update(fp: Fingerprint)

    @Delete
    suspend fun delete(fp: Fingerprint)

    @Query("DELETE FROM fingerprints")
    suspend fun deleteAll()

}
