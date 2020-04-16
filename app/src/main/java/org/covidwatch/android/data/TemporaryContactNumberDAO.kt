package org.covidwatch.android.data

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*

@Dao
interface TemporaryContactNumberDAO {
    @get:Query("SELECT * FROM temporary_contact_numbers")
    val all: List<TemporaryContactNumber>

    @get:Query("SELECT * FROM temporary_contact_numbers ORDER BY foundDate DESC")
    val allSortedByDescTimestamp: LiveData<List<TemporaryContactNumber>>

    @get:Query("SELECT * FROM temporary_contact_numbers ORDER BY foundDate DESC")
    val pagedAllSortedByDescTimestamp: DataSource.Factory<Int, TemporaryContactNumber>

    @Query("SELECT * FROM temporary_contact_numbers WHERE bytes = :identifier")
    fun findByPrimaryKey(identifier: String): TemporaryContactNumber

    @Query("SELECT * FROM temporary_contact_numbers WHERE was_potentially_infectious = '1' LIMIT 1")
    fun findFirstPotentiallyInfections(): LiveData<TemporaryContactNumber>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(temporaryContactNumber: TemporaryContactNumber)

    @Update
    fun update(temporaryContactNumber: TemporaryContactNumber)

    @Update
    fun update(temporaryContactNumbers: List<TemporaryContactNumber>)

    @Query("UPDATE temporary_contact_numbers SET was_potentially_infectious = :wasPotentiallyInfectious WHERE bytes IN (:identifiers)")
    fun update(identifiers: List<ByteArray>, wasPotentiallyInfectious: Boolean)

    @Query("UPDATE temporary_contact_numbers SET was_potentially_infectious = '1'")
    fun markAllAsPotentiallyInfectious()

//    @Query("UPDATE temporary_contact_numbers SET upload_state = :uploadState WHERE bytes IN (:identifiers)")
//    fun update(identifiers: List<ByteArray>, uploadState: Int)
//
    @Query("DELETE FROM temporary_contact_numbers")
    fun deleteAll()
}