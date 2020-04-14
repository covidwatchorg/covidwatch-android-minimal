package org.covidwatch.android.data.firestore

import java.util.*
import java.util.concurrent.TimeUnit

object FirestoreConstants {

    const val COLLECTION_TEMPORARY_CONTACT_NUMBERS: String = "temporary_contact_numbers"

    const val FIELD_TIMESTAMP: String = "timestamp"

    // Only fetch temporary contact numbers from the past 2 weeks
    private const val OLDEST_PUBLIC_TEMPORARY_CONTACT_NUMBERS_TO_FETCH_SECONDS: Long = 60 * 60 * 24 * 7 * 2

    fun lastFetchTime(): Date {
        val fetchTime = Date()
        fetchTime.time -= TimeUnit.SECONDS.toMillis(OLDEST_PUBLIC_TEMPORARY_CONTACT_NUMBERS_TO_FETCH_SECONDS)
        return fetchTime
    }
}
