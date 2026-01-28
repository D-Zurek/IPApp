package com.example.app1
import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScannerActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var recycler: RecyclerView
    private lateinit var btnScan: Button
    private val adapter = ScanAdapter()

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val results = wifiManager.scanResults
            adapter.update(results)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) ensureLocationEnabledAndScan()
        else AlertDialog.Builder(this)
            .setTitle("Brak uprawnień")
            .setMessage("Aplikacja potrzebuje uprawnień lokalizacji, aby skanować sieci Wi-Fi.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        recycler = findViewById(R.id.recycler)
        btnScan = findViewById(R.id.btnScan)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        registerReceiver(
            scanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        btnScan.setOnClickListener { checkPermissionsAndScan() }
    }

    private fun checkPermissionsAndScan() {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            coarse != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else ensureLocationEnabledAndScan()
    }

    @SuppressLint("MissingPermission")
    private fun ensureLocationEnabledAndScan() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsEnabled) {
            AlertDialog.Builder(this)
                .setTitle("Włącz lokalizację")
                .setMessage("Aby wykonać skan Wi-Fi, proszę włączyć lokalizację w ustawieniach.")
                .setPositiveButton("Otwórz ustawienia") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Anuluj", null)
                .show()
            return
        }

        if (!wifiManager.isWifiEnabled) wifiManager.isWifiEnabled = true
        val success = wifiManager.startScan()
        if (!success) adapter.update(wifiManager.scanResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanReceiver)
    }

    class ScanAdapter : RecyclerView.Adapter<ScanAdapter.VH>() {
        private val items = mutableListOf<ScanResult>()
        fun update(list: List<ScanResult>) {
            items.clear()
            items.addAll(list.sortedByDescending { it.level })
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_ap, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val r = items[position]
            holder.ssid.text = r.SSID.ifEmpty { "<ukryte SSID>" }
            holder.details.text = "BSSID: ${r.BSSID}  RSSI: ${r.level} dBm  Freq: ${r.frequency} MHz"
        }
        override fun getItemCount() = items.size
        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val ssid: TextView = view.findViewById(R.id.tvSsid)
            val details: TextView = view.findViewById(R.id.tvDetails)
        }
    }
}
