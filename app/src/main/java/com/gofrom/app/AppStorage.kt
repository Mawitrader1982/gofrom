package com.gofrom.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class StoredProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val photoUri: String? = null
)

data class StoredMeal(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val date: String = LocalDate.now().toString(),
    val name: String,
    val calories: Int? = null,
    val photoUri: String? = null
)

class AppStorage(context: Context) {
    private val prefs = context.getSharedPreferences("gofrom_data", Context.MODE_PRIVATE)

    fun profiles(): List<StoredProfile> = jsonArray("profiles").mapNotNull { item ->
        runCatching {
            StoredProfile(
                id = item.getString("id"),
                name = item.getString("name"),
                email = item.optString("email"),
                photoUri = item.optString("photoUri").takeIf(String::isNotBlank)
            )
        }.getOrNull()
    }

    fun saveProfiles(profiles: List<StoredProfile>) {
        val array = JSONArray()
        profiles.forEach { profile -> array.put(JSONObject().apply {
            put("id", profile.id); put("name", profile.name); put("email", profile.email)
            profile.photoUri?.let { put("photoUri", it) }
        }) }
        prefs.edit().putString("profiles", array.toString()).apply()
    }

    fun currentProfileId(): String? = prefs.getString("current_profile_id", null)
    fun setCurrentProfile(id: String?) = prefs.edit().putString("current_profile_id", id).apply()

    fun meals(profileId: String): List<StoredMeal> = jsonArray("meals").mapNotNull { item ->
        runCatching {
            StoredMeal(
                id = item.getString("id"), profileId = item.getString("profileId"),
                date = item.getString("date"), name = item.getString("name"),
                calories = item.optInt("calories").takeIf { item.has("calories") },
                photoUri = item.optString("photoUri").takeIf(String::isNotBlank)
            )
        }.getOrNull()
    }.filter { it.profileId == profileId }

    fun addMeal(meal: StoredMeal) {
        val all = jsonArray("meals").toMutableList()
        all += JSONObject().apply {
            put("id", meal.id); put("profileId", meal.profileId); put("date", meal.date); put("name", meal.name)
            meal.calories?.let { put("calories", it) }; meal.photoUri?.let { put("photoUri", it) }
        }
        prefs.edit().putString("meals", JSONArray(all).toString()).apply()
    }

    private fun jsonArray(key: String): List<JSONObject> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]")); (0 until array.length()).map { array.getJSONObject(it) }
    }.getOrDefault(emptyList())
}
