package com.example.app1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fingerprints")
data class Fingerprint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ssids: String,
    val bssid: String,
    val rssis: String,
    val posX: Float,
    val posY: Float
)
