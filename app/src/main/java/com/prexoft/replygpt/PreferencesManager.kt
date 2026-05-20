package com.prexoft.replygpt

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getEnabledPackages(): Set<String> {
        return prefs.getStringSet("enabled_packages", emptySet()) ?: emptySet()
    }

    fun setEnabledPackages(packages: Set<String>) {
        prefs.edit().putStringSet("enabled_packages", packages).apply()
    }

    fun getUserInfo(): String {
        return prefs.getString("user_info", "") ?: ""
    }

    fun setUserInfo(info: String) {
        prefs.edit().putString("user_info", info).apply()
    }

    fun incrementDailyReplyCount() {
        val today = SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
        val currentCount = prefs.getInt("replies_$today", 0)
        prefs.edit().putInt("replies_$today", currentCount + 1).apply()
    }

    fun getDailyReplyCount(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
        return prefs.getInt("replies_$today", 0)
    }

    fun getWeeklyReplyCounts(): List<Int> {
        val counts = mutableListOf<Int>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            val dateKey = dateFormat.format(calendar.time)
            counts.add(prefs.getInt("replies_$dateKey", 0))
        }
        return counts
    }
}