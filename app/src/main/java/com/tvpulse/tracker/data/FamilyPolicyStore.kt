package com.tvpulse.tracker.data

import android.content.Context
import android.content.SharedPreferences
import com.tvpulse.tracker.model.AppCategory
import com.tvpulse.tracker.model.CategoryAccessPolicy
import com.tvpulse.tracker.model.FamilyMemberProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FamilyPolicyStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tv_pulse_policy_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROFILES_JSON = "profiles_json_v3"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id_v3"
        private const val KEY_PARENTAL_PIN = "parental_pin"
        private const val KEY_DAILY_FAMILY_QUOTA_MIN = "daily_family_quota_min"
        private const val KEY_BLUE_LIGHT_ACTIVE = "blue_light_active"
        private const val KEY_BLUE_LIGHT_AUTO_HOUR = "blue_light_auto_hour"
        private const val KEY_BLUE_LIGHT_AUTO_MINUTE = "blue_light_auto_minute"
        private const val KEY_BLUE_LIGHT_ACCUMULATED_MIN = "blue_light_accumulated_min_v2"
        private const val KEY_APP_LIMITS_JSON = "app_limits_json"
        private const val KEY_CURFEW_APPS_SET = "curfew_apps_set"
        private const val KEY_CATEGORY_POLICIES_JSON = "category_policies_json"
        private const val KEY_BLINK_REMINDER_DATE = "blink_reminder_date"
        private const val KEY_BLINK_REMINDER_COUNT = "blink_reminder_count"
        private const val KEY_MEMBER_EXTRA_MINUTES_JSON = "member_extra_minutes_json"
    }

    fun getProfiles(): List<FamilyMemberProfile> {
        val raw = prefs.getString(KEY_PROFILES_JSON, null)
        if (raw.isNullOrBlank()) {
            val defaults = defaultProfiles()
            saveProfiles(defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<FamilyMemberProfile>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val catsArr = obj.optJSONArray("preferredCategories") ?: JSONArray()
                val cats = mutableSetOf<AppCategory>()
                for (j in 0 until catsArr.length()) {
                    runCatching { AppCategory.valueOf(catsArr.getString(j)) }.getOrNull()?.let { cats.add(it) }
                }
                list.add(
                    FamilyMemberProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        ageOrRoleTag = obj.getString("ageOrRoleTag"),
                        subtitle = obj.getString("subtitle"),
                        isRestricted = obj.getBoolean("isRestricted"),
                        weekdayLimitMinutes = obj.getInt("weekdayLimitMinutes"),
                        weekendLimitMinutes = obj.getInt("weekendLimitMinutes"),
                        curfewEnabled = obj.getBoolean("curfewEnabled"),
                        curfewStartHour = obj.optInt("curfewStartHour", 21),
                        curfewEndHour = obj.optInt("curfewEndHour", 7),
                        eyeRestEnabled = obj.optBoolean("eyeRestEnabled", true),
                        eyeRestIntervalMinutes = obj.optInt("eyeRestIntervalMinutes", 40),
                        eyeRestDurationMinutes = obj.optInt("eyeRestDurationMinutes", 10),
                        preferredCategories = cats.ifEmpty { setOf(AppCategory.VIDEO) }
                    )
                )
            }
            if (list.isEmpty()) defaultProfiles() else list
        } catch (e: Exception) {
            defaultProfiles()
        }
    }

    fun saveProfiles(profiles: List<FamilyMemberProfile>) {
        val arr = JSONArray()
        profiles.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("ageOrRoleTag", p.ageOrRoleTag)
                put("subtitle", p.subtitle)
                put("isRestricted", p.isRestricted)
                put("weekdayLimitMinutes", p.weekdayLimitMinutes)
                put("weekendLimitMinutes", p.weekendLimitMinutes)
                put("curfewEnabled", p.curfewEnabled)
                put("curfewStartHour", p.curfewStartHour)
                put("curfewEndHour", p.curfewEndHour)
                put("eyeRestEnabled", p.eyeRestEnabled)
                put("eyeRestIntervalMinutes", p.eyeRestIntervalMinutes)
                put("eyeRestDurationMinutes", p.eyeRestDurationMinutes)
                val cats = JSONArray()
                p.preferredCategories.forEach { cats.put(it.name) }
                put("preferredCategories", cats)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES_JSON, arr.toString()).apply()
    }

    fun updateProfile(updated: FamilyMemberProfile) {
        val current = getProfiles().map { if (it.id == updated.id) updated else it }
        saveProfiles(current)
    }

    fun addProfile(name: String, ageTag: String, isRestricted: Boolean, weekdayMin: Int, weekendMin: Int): FamilyMemberProfile {
        val current = getProfiles().toMutableList()
        val newProfile = FamilyMemberProfile(
            id = "member_${System.currentTimeMillis()}",
            name = name.ifBlank { "新成员" },
            ageOrRoleTag = ageTag.ifBlank { "成员" },
            subtitle = if (isRestricted) "独立限额护眼模式" else "标准观影模式",
            isRestricted = isRestricted,
            weekdayLimitMinutes = weekdayMin,
            weekendLimitMinutes = weekendMin,
            curfewEnabled = isRestricted,
            curfewStartHour = 21,
            curfewEndHour = 7,
            eyeRestEnabled = true,
            eyeRestIntervalMinutes = 45,
            eyeRestDurationMinutes = 10,
            preferredCategories = if (isRestricted) setOf(AppCategory.KIDS, AppCategory.EDUCATION) else setOf(AppCategory.VIDEO)
        )
        current.add(newProfile)
        saveProfiles(current)
        return newProfile
    }

    fun getActiveProfileId(): String {
        return prefs.getString(KEY_ACTIVE_PROFILE_ID, "owner_master") ?: "owner_master"
    }

    fun setActiveProfileId(profileId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
    }

    fun getParentalPin(): String {
        return prefs.getString(KEY_PARENTAL_PIN, "0000") ?: "0000"
    }

    fun setParentalPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            prefs.edit().putString(KEY_PARENTAL_PIN, newPin).apply()
        }
    }

    fun getDailyFamilyQuotaMinutes(): Int {
        return prefs.getInt(KEY_DAILY_FAMILY_QUOTA_MIN, 300) // Default 5h 00m = 300m
    }

    fun setDailyFamilyQuotaMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_FAMILY_QUOTA_MIN, minutes.coerceIn(60, 720)).apply()
    }

    fun isBlueLightFilterActive(): Boolean {
        return prefs.getBoolean(KEY_BLUE_LIGHT_ACTIVE, false)
    }

    fun setBlueLightFilterActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_BLUE_LIGHT_ACTIVE, active).apply()
    }

    fun getBlueLightAutoSchedule(): Pair<Int, Int> {
        val h = prefs.getInt(KEY_BLUE_LIGHT_AUTO_HOUR, 21)
        val m = prefs.getInt(KEY_BLUE_LIGHT_AUTO_MINUTE, 30)
        return h to m
    }

    fun setBlueLightAutoSchedule(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_BLUE_LIGHT_AUTO_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_BLUE_LIGHT_AUTO_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun getBlueLightAccumulatedMinutes(): Int {
        return prefs.getInt(KEY_BLUE_LIGHT_ACCUMULATED_MIN, 0)
    }

    fun addBlueLightActiveMinutes(deltaMinutes: Int) {
        val current = getBlueLightAccumulatedMinutes()
        prefs.edit().putInt(KEY_BLUE_LIGHT_ACCUMULATED_MIN, current + deltaMinutes).apply()
    }

    fun getAppDailyLimits(): Map<String, Int> {
        val raw = prefs.getString(KEY_APP_LIMITS_JSON, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getInt(k)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setAppDailyLimit(packageName: String, limitMinutes: Int) {
        val current = getAppDailyLimits().toMutableMap()
        if (limitMinutes <= 0) {
            current.remove(packageName)
        } else {
            current[packageName] = limitMinutes
        }
        val obj = JSONObject()
        current.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_APP_LIMITS_JSON, obj.toString()).apply()
    }

    fun getCurfewApps(): Set<String> {
        return prefs.getStringSet(KEY_CURFEW_APPS_SET, emptySet()) ?: emptySet()
    }

    fun toggleAppCurfew(packageName: String): Boolean {
        val current = getCurfewApps().toMutableSet()
        val added = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        prefs.edit().putStringSet(KEY_CURFEW_APPS_SET, current).apply()
        return added
    }

    fun getCategoryPolicies(): Map<AppCategory, CategoryAccessPolicy> {
        val raw = prefs.getString(KEY_CATEGORY_POLICIES_JSON, null)
        val defaults = mapOf(
            AppCategory.KIDS to CategoryAccessPolicy.ALLOW,
            AppCategory.EDUCATION to CategoryAccessPolicy.UNLIMITED,
            AppCategory.GAME_FITNESS to CategoryAccessPolicy.PIN_LOCK,
            AppCategory.VIDEO to CategoryAccessPolicy.ALLOW,
            AppCategory.SYSTEM_CAST to CategoryAccessPolicy.PIN_LOCK
        )
        if (raw.isNullOrBlank()) return defaults
        return try {
            val obj = JSONObject(raw)
            val result = defaults.toMutableMap()
            AppCategory.entries.forEach { cat ->
                if (obj.has(cat.name)) {
                    runCatching { CategoryAccessPolicy.valueOf(obj.getString(cat.name)) }
                        .getOrNull()?.let { result[cat] = it }
                }
            }
            result
        } catch (e: Exception) {
            defaults
        }
    }

    fun cycleCategoryPolicy(category: AppCategory): CategoryAccessPolicy {
        val current = getCategoryPolicies().toMutableMap()
        val order = listOf(
            CategoryAccessPolicy.ALLOW,
            CategoryAccessPolicy.UNLIMITED,
            CategoryAccessPolicy.PIN_LOCK,
            CategoryAccessPolicy.BLOCKED
        )
        val curPolicy = current[category] ?: CategoryAccessPolicy.ALLOW
        val nextPolicy = order[(order.indexOf(curPolicy) + 1) % order.size]
        current[category] = nextPolicy
        val obj = JSONObject()
        current.forEach { (k, v) -> obj.put(k.name, v.name) }
        prefs.edit().putString(KEY_CATEGORY_POLICIES_JSON, obj.toString()).apply()
        return nextPolicy
    }

    fun getTodayBlinkReminderCount(longestContinuousMinutes: Int, intervalMinutes: Int): Int {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(KEY_BLINK_REMINDER_DATE, "")
        val savedCount = if (savedDate == todayStr) prefs.getInt(KEY_BLINK_REMINDER_COUNT, 0) else 0
        val computedFromUsage = if (intervalMinutes > 0) (longestContinuousMinutes / intervalMinutes) else 0
        val finalCount = maxOf(savedCount, computedFromUsage)
        if (finalCount != savedCount || savedDate != todayStr) {
            prefs.edit()
                .putString(KEY_BLINK_REMINDER_DATE, todayStr)
                .putInt(KEY_BLINK_REMINDER_COUNT, finalCount)
                .apply()
        }
        return finalCount
    }

    fun incrementBlinkReminderToday() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val cur = getTodayBlinkReminderCount(0, 45)
        prefs.edit()
            .putString(KEY_BLINK_REMINDER_DATE, todayStr)
            .putInt(KEY_BLINK_REMINDER_COUNT, cur + 1)
            .apply()
    }

    private fun defaultProfiles(): List<FamilyMemberProfile> = listOf(
        FamilyMemberProfile(
            id = "owner_master",
            name = "主人",
            ageOrRoleTag = "本机",
            subtitle = "默认主账号",
            isRestricted = false,
            weekdayLimitMinutes = 0,
            weekendLimitMinutes = 0,
            curfewEnabled = false,
            curfewStartHour = 23,
            curfewEndHour = 6,
            eyeRestEnabled = true,
            eyeRestIntervalMinutes = 60,
            eyeRestDurationMinutes = 5,
            preferredCategories = setOf(
                AppCategory.VIDEO,
                AppCategory.KIDS,
                AppCategory.EDUCATION,
                AppCategory.GAME_FITNESS,
                AppCategory.SYSTEM_CAST
            )
        )
    )
}
