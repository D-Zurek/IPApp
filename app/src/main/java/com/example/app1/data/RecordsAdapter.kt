package com.example.app1.data

import com.example.app1.R
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView

class RecordsAdapter(
    private val onDelete: (Fingerprint) -> Unit,
    private val onEdit: (Fingerprint) -> Unit
) : RecyclerView.Adapter<RecordsAdapter.VH>() {

    private val data = mutableListOf<Fingerprint>()

    fun setData(list: List<Fingerprint>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_record, parent, false)
        return VH(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(data[position])
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val txtInfo = view.findViewById<TextView>(R.id.txtInfo)
        private val btnEdit = view.findViewById<Button>(R.id.btnEdit)
        private val btnDelete = view.findViewById<Button>(R.id.btnDelete)

        fun bind(fp: Fingerprint) {
            txtInfo.text = """
                ID: ${fp.id}
                X=${fp.posX}  Y=${fp.posY}
                SSID: ${fp.ssids}
                RSSI: ${fp.rssis}
            """.trimIndent()

            btnDelete.setOnClickListener { onDelete(fp) }
            btnEdit.setOnClickListener { onEdit(fp) }
        }
    }
}
