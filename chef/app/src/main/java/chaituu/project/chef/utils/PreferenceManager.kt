package chaituu.project.chef.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "expresso_chef",
        Context.MODE_PRIVATE
    )

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun clearToken() {
        prefs.edit().remove("token").apply()
    }

    fun saveCafeId(cafeId: String) {
        prefs.edit().putString("cafeId", cafeId).apply()
    }

    fun getCafeId(): String? {
        return prefs.getString("cafeId", null)
    }

    fun saveChefName(name: String) {
        prefs.edit().putString("chefName", name).apply()
    }

    fun getChefName(): String? {
        return prefs.getString("chefName", null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
