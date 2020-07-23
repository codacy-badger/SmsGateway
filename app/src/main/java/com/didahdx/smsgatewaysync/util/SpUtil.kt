package com.didahdx.smsgatewaysync.utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.core.text.isDigitsOnly
import androidx.preference.PreferenceManager


/**
 * Shared preference helper
 * */

object SpUtil {
    const val PREF_NAME = "SettingPreferences"
    const val POSITION = "position"
    const val QUERY = "query"

    @JvmStatic
    private fun getPrefs(context: Context): SharedPreferences {
//        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @JvmStatic
    fun getPreferenceString(context: Context, key: String,defaultValue: String): String {
        return getPrefs(context).getString(key, defaultValue) ?: defaultValue
    }

    fun getPreferenceInt(context: Context, key: String,defaultValue:Int ): Int {
        return getPrefs(context).getInt(key, defaultValue)
    }

    fun getPreferenceBoolean(context: Context, key: String): Boolean {
        return getPrefs(context).getBoolean(key, false)
    }

    fun setPreferenceString(context: Context, key: String, value: String) {
        val editor = getPrefs(context).edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun setPreferenceInt(context: Context, key: String, value: Int) {
        val editor = getPrefs(context).edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun setPreferenceBoolean(context: Context, key: String, value: Boolean) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getQueryList(context: Context): ArrayList<String> {
        val queryList = ArrayList<String>()
        for (i in 1..5) {
            var query =
                getPrefs(context).getString(QUERY + i.toString(), "")
            if (query!!.isNotEmpty()) {
                query = query.replace(",", " ")
                queryList.add(query.trim { it <= ' ' })
            }
        }
        return queryList
    }

    fun updateSettingsRemotely(context: Context, type: String, key: String, value: String) {
        when (type) {
            PREF_TYPE_BOOLEAN -> {
                if (value == "0") {
                    setPreferenceBoolean(context, key, false)
                } else if (value == "1") {
                    setPreferenceBoolean(context, key, true)
                }
            }

            PREF_TYPE_STRING -> {
                setPreferenceString(context, key, value)
            }

            PREF_TYPE_INT -> {
                if (value.trim().isDigitsOnly()) {
                    try {
                        setPreferenceInt(context, key, Integer.parseInt(value))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

        }
    }
}
