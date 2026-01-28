package com.example.app1.data

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import kotlinx.coroutines.launch


import android.widget.Button

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts


class RecordsActivity : AppCompatActivity() {

    private lateinit var adapter: RecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        //---------EXPORT------------

        val btnExport = findViewById<Button>(R.id.btnExportCsv)
        btnExport.setOnClickListener {
            exportToCsv()
        }

        //---------IMPORT------------

        val btnImport = findViewById<Button>(R.id.btnImportCsv)
        val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) importCsv(uri)
            }
        }

        btnImport.setOnClickListener {

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            }
            importLauncher.launch(intent)
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerRecords)

        adapter = RecordsAdapter(
            onDelete = { delete(it) },
            onEdit = { edit(it) }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val list = AppDatabase.getDatabase(this@RecordsActivity)
                .fingerprintDao()
                .getAll()

            adapter.setData(list)
        }
    }

    private fun delete(fp: Fingerprint) {
        lifecycleScope.launch {
            AppDatabase.getDatabase(this@RecordsActivity)
                .fingerprintDao()
                .delete(fp)

            loadData()
        }
    }

    private fun edit(fp: Fingerprint) {
        Toast.makeText(
            this,
            "Edycja ID=${fp.id} (edycja)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun writeCsv(output: java.io.OutputStream, data: List<Fingerprint>) {
        output.write("ID,SSIDs,BSSID,RSSIs,PosX,PosY\n".toByteArray())

        data.forEach { fp ->
            val line =
                "${fp.id},\"${fp.ssids}\",\"${fp.bssid}\",\"${fp.rssis}\",${fp.posX},${fp.posY}\n"
            output.write(line.toByteArray())
        }
    }

    private fun exportToCsv() {
        lifecycleScope.launch {
            try {
                val dao = AppDatabase.getDatabase(this@RecordsActivity)
                    .fingerprintDao()

                val fingerprints = dao.getAll()

                if (fingerprints.isEmpty()) {
                    Toast.makeText(this@RecordsActivity, "Brak danych do zapisania", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val fileName = "fingerprints_${System.currentTimeMillis()}.csv"

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                }

                val uri = contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw Exception("Nie udało się utworzyć pliku")

                contentResolver.openOutputStream(uri)?.use { output ->
                    writeCsv(output, fingerprints)
                }

                Toast.makeText(
                    this@RecordsActivity,
                    "Zapisano do Pobranych: $fileName",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(this@RecordsActivity, "Błąd zapisu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val regex = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")
        return regex.split(line).map { it.trim().removeSurrounding("\"") }
    }


    private fun importCsv(uri: Uri) {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@RecordsActivity).fingerprintDao()


            dao.deleteAll()

            val inputStream = contentResolver.openInputStream(uri)
            val reader = inputStream?.bufferedReader() ?: return@launch

            val lines = reader.readLines()
            for ((index, line) in lines.withIndex()) {
                if (index == 0) continue

                val parts = parseCsvLine(line)

                if (parts.size < 6) continue

                val fp = Fingerprint(
                    ssids = parts[1].trim().removeSurrounding("\""),
                    bssid = parts[2].trim().removeSurrounding("\""),
                    rssis = parts[3].trim().removeSurrounding("\""),
                    posX = parts[4].toFloatOrNull() ?: 0f,
                    posY = parts[5].toFloatOrNull() ?: 0f
                )


                dao.insert(fp)
            }

            val allData = dao.getAll()
            adapter.setData(allData)

            Toast.makeText(this@RecordsActivity, "Import zakończony!", Toast.LENGTH_LONG).show()
        }
    }





}