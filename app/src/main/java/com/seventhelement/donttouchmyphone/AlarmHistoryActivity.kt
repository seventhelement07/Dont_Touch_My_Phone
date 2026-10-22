package com.seventhelement.donttouchmyphone

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.seventhelement.donttouchmyphone.databinding.ActivityAlarmHistoryBinding
import com.seventhelement.donttouchmyphone.db.AlarmDatabase
import com.seventhelement.donttouchmyphone.db.AlarmEvent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Alarm History"

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)

        loadEvents()
    }

    private fun loadEvents() {
        lifecycleScope.launch {
            val events = AlarmDatabase.getInstance(this@AlarmHistoryActivity)
                .alarmHistoryDao()
                .getAllEvents()
            if (events.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
                binding.historyRecyclerView.adapter = HistoryAdapter(events)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_alarm_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_history) {
            confirmClearHistory()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmClearHistory() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear History")
            .setMessage("Delete all recorded alarm events? This can't be undone.")
            .setPositiveButton("Clear") { _, _ ->
                lifecycleScope.launch {
                    AlarmDatabase.getInstance(this@AlarmHistoryActivity)
                        .alarmHistoryDao()
                        .deleteAllEvents()
                    loadEvents()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    inner class HistoryAdapter(private val events: List<AlarmEvent>) :
        RecyclerView.Adapter<HistoryAdapter.VH>() {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val triggerType: TextView = itemView.findViewById(R.id.triggerType)
            val dateTime: TextView = itemView.findViewById(R.id.dateTime)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(R.layout.item_alarm_event, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val event = events[position]
            holder.triggerType.text = when (event.triggerType) {
                "Charger" -> "Charger Removed"
                else -> "Motion Detected"
            }
            holder.dateTime.text = dateFormat.format(Date(event.timestamp))
        }

        override fun getItemCount() = events.size
    }
}
