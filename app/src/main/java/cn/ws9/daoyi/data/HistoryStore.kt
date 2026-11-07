package cn.ws9.daoyi.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class HistoryEntry(val time: String, val syndrome: String, val formula: String, val source: String)

class HistoryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("daoyi_history", Context.MODE_PRIVATE)

    fun all(): List<HistoryEntry> = runCatching {
        val arr = JSONArray(prefs.getString("items", "[]"))
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            HistoryEntry(
                o.optString("time", ""), o.optString("syndrome", ""),
                o.optString("formula", ""), o.optString("source", "")
            )
        }
    }.getOrDefault(emptyList())

    fun add(syndrome: String, formula: String, source: String) {
        runCatching {
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            val arr = JSONArray(prefs.getString("items", "[]"))
            val item = JSONObject()
                .put("time", now).put("syndrome", syndrome)
                .put("formula", formula).put("source", source)
            val merged = JSONArray().put(item)
            for (i in 0 until minOf(arr.length(), 9)) merged.put(arr.optJSONObject(i))
            prefs.edit().putString("items", merged.toString()).apply()
        }
    }

    fun clear() = prefs.edit().remove("items").apply()
}
