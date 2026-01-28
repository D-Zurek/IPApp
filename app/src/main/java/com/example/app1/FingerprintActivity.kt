package com.example.app1

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.data.AppDatabase
import com.example.app1.data.Fingerprint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FingerprintActivity : AppCompatActivity() {

    private lateinit var recyclerAp: RecyclerView
    private lateinit var editX: EditText
    private lateinit var editY: EditText
    private lateinit var btnSave: Button
    private lateinit var wifiManager: WifiManager
    private lateinit var adapter: ApCheckboxAdapter

    private var lastScanResults: List<ScanResult> = emptyList()


    private var pendingSave = false
    private var pendingX = 0f
    private var pendingY = 0f

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            lastScanResults = wifiManager.scanResults


            adapter.updateScanResults(lastScanResults)


            if (pendingSave) {
                pendingSave = false
                saveFingerprintInternal(pendingX, pendingY)
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) wifiManager.startScan()
            else Toast.makeText(this, "Brak uprawnień lokalizacji", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint)

        recyclerAp = findViewById(R.id.recyclerAp)
        editX = findViewById(R.id.editX)
        editY = findViewById(R.id.editY)
        btnSave = findViewById(R.id.btnSaveFingerprint)

        recyclerAp.layoutManager = LinearLayoutManager(this)
        adapter = ApCheckboxAdapter(emptyList())
        recyclerAp.adapter = adapter

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        requestPermissionAndScan()

        btnSave.setOnClickListener { startFreshScanAndSave() }
    }

    private fun requestPermissionAndScan() {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION
        if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(perm)
        } else {
            wifiManager.startScan()
        }
    }

    private fun startFreshScanAndSave() {
        val posX = editX.text.toString().toFloatOrNull()
        val posY = editY.text.toString().toFloatOrNull()

        if (posX == null || posY == null) {
            Toast.makeText(this, "Podaj poprawne X i Y", Toast.LENGTH_SHORT).show()
            return
        }

        val selected = adapter.getSelected()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Wybierz przynajmniej jedno AP", Toast.LENGTH_SHORT).show()
            return
        }

        pendingSave = true
        pendingX = posX
        pendingY = posY

        Toast.makeText(this, "Odświeżam skan WiFi...", Toast.LENGTH_SHORT).show()
        wifiManager.startScan()
    }

    private fun saveFingerprintInternal(posX: Float, posY: Float) {
        val selected = adapter.getSelected()

        if (selected.isEmpty()) {
            Toast.makeText(this, "Brak wybranych AP", Toast.LENGTH_SHORT).show()
            return
        }

        val ssids = selected.joinToString(",") { it.SSID }
        val rssis = selected.joinToString(",") { it.level.toString() }
        val bssid = selected.joinToString(",") { it.BSSID }

        val fingerprint = Fingerprint(
            ssids = ssids,
            bssid = bssid,
            rssis = rssis,
            posX = posX,
            posY = posY
        )

        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@FingerprintActivity)
                .fingerprintDao()
                .insert(fingerprint)
        }

        Toast.makeText(this, "Fingerprint zapisany", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
