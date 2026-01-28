package com.example.app1.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt
import com.example.app1.R

class LocationActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var tvPosition: TextView
    private lateinit var btnRefresh: Button

    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 5000L

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val results = wifiManager.scanResults
            calculatePosition(results)
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            startScan()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        tvPosition = findViewById(R.id.tvPosition)
        btnRefresh = findViewById(R.id.btnRefresh)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        registerReceiver(scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        handler.post(refreshRunnable)

        btnRefresh.setOnClickListener {
            startScan()
        }
    }

    private fun startScan() {
        if (!wifiManager.isWifiEnabled) wifiManager.isWifiEnabled = true
        wifiManager.startScan()
    }

    private fun calculatePosition(scanResults: List<ScanResult>) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@LocationActivity)
            val fingerprints = db.fingerprintDao().getAll()

            if (fingerprints.isEmpty()) {
                tvPosition.text = "Brak zapisanych fingerprintów!"
                return@launch
            }

            val currentMap = scanResults.associate { it.BSSID to it.level.toFloat() }

            var nearestDistance = Float.MAX_VALUE
            var nearestX = 0f
            var nearestY = 0f

            for (fp in fingerprints) {
                val rssisSaved = fp.rssis.split(",").map { it.toFloat() }
                val bssidsSaved = fp.bssid.split(",")

                val commonRssis = mutableListOf<Pair<Float, Float>>()
                for ((index, bssid) in bssidsSaved.withIndex()) {
                    currentMap[bssid]?.let { currentRssi ->
                        commonRssis.add(currentRssi to rssisSaved[index])
                    }
                }

                if (commonRssis.isEmpty()) continue

                val distance = sqrt(
                    commonRssis.sumOf { (c, s) -> (c - s).toDouble().pow(2) }
                ).toFloat()

                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearestX = fp.posX
                    nearestY = fp.posY
                }
            }

            tvPosition.text = "Szacowana pozycja:\nX = $nearestX\nY = $nearestY"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanReceiver)
        handler.removeCallbacks(refreshRunnable)
    }
}
