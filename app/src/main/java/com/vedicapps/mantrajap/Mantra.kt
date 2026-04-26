package com.vedicapps.mantrajap

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
@Entity(tableName = "mantras")
data class Mantra(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var name: String = "",
    var count: Int = 0,
    var target: Int = 1,
    var audioPath: String? = null,
    var isFixed: Boolean = false,
    var remoteKey: String = ""
) {
    // This empty constructor is CRITICAL for Firebase Realtime DB to work
    constructor() : this(0L, "", 0, 1, null, false, "")
}