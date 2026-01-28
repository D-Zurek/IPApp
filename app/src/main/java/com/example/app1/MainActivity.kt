package com.example.app1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.app1.data.RecordsActivity
import com.example.app1.data.LocationActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnScanner: Button
    private lateinit var btnFingerprint: Button
    private lateinit var btnRecords: Button


git
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScanner = findViewById(R.id.btnScanner)
        btnScanner.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        btnFingerprint = findViewById(R.id.btnFingerprint)
        btnFingerprint.setOnClickListener {
            startActivity(Intent(this, FingerprintActivity::class.java))
        }

        btnRecords = findViewById(R.id.btnRecords)
        btnRecords.setOnClickListener {
            startActivity(Intent(this, RecordsActivity::class.java))
        }

        val btnLocation = findViewById<Button>(R.id.btnLocation)
        btnLocation.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }


    }
}
