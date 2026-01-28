package com.example.app1

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Wrapper dla ScanResult z informacją o zaznaczeniu
data class ApItem(
    val scanResult: ScanResult,
    var isSelected: Boolean = false
)

class ApCheckboxAdapter(scanResults: List<ScanResult>) :
    RecyclerView.Adapter<ApCheckboxAdapter.VH>() {

    private val items = mutableListOf<ApItem>().apply {
        addAll(scanResults.map { ApItem(it) })
    }

    fun getSelected(): List<ScanResult> =
        items.filter { it.isSelected }.map { it.scanResult }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ap_checkbox, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val apItem = items[position]
        holder.ssid.text = apItem.scanResult.SSID.ifEmpty { "<ukryte>" }

        // usuń stary listener zanim ustawisz checkbox, aby uniknąć callback loop
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = apItem.isSelected
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            apItem.isSelected = isChecked
        }
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ssid: TextView = view.findViewById(R.id.tvSsid)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
    }

    fun updateScanResults(newResults: List<ScanResult>) {
        // zachowaj zaznaczone BSSID
        val selectedBssids = getSelected().map { it.BSSID }.toSet()

        items.clear()
        items.addAll(newResults.map { scan ->
            ApItem(scan, selectedBssids.contains(scan.BSSID))
        })

        notifyDataSetChanged()
    }
}
