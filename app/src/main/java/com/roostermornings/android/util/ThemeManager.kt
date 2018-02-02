package com.roostermornings.android.util

import android.app.Activity
import android.content.SharedPreferences
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import com.roostermornings.android.R
import java.util.*

/**
 * Created by bscholtz on 2018/02/02.
 */
class ThemeManager(val activity: Activity) {

    fun setDayNightTheme(defaultSharedPreferences: SharedPreferences): Boolean {
        val calendar = Calendar.getInstance()

        val dayNightThemeArrayEntries = activity.resources.getStringArray(R.array.user_settings_day_night_theme_entry_values)
        val dayNightThemeSetting = defaultSharedPreferences.getString(Constants.USER_SETTINGS_DAY_NIGHT_THEME, "")

        when(dayNightThemeSetting) {
        // Automatic theme
            dayNightThemeArrayEntries[0] -> {
                if (calendar.get(Calendar.HOUR_OF_DAY) >= 18 || calendar.get(Calendar.HOUR_OF_DAY) < 7) {
                    return setThemeNight()
                } else if (calendar.get(Calendar.HOUR_OF_DAY) >= 7) {
                    return setThemeDay()
                }
            }
        // Day theme
            dayNightThemeArrayEntries[1] -> return setThemeDay()
        // Night theme
            dayNightThemeArrayEntries[2] -> return setThemeNight()
        // Automatic theme
            else -> {
                if (calendar.get(Calendar.HOUR_OF_DAY) >= 18 || calendar.get(Calendar.HOUR_OF_DAY) < 7) {
                    return setThemeNight()
                } else if (calendar.get(Calendar.HOUR_OF_DAY) >= 7) {
                    return setThemeDay()
                }
            }
        }
        return false
    }

    private fun setThemeDay(): Boolean {
        return try {
            activity.findViewById<View>(R.id.main_content)?.isSelected = false
            activity.findViewById<View>(R.id.toolbar)?.setBackgroundColor(ResourcesCompat.getColor(activity.resources, R.color.rooster_blue, null))
            activity.findViewById<View>(R.id.tabs)?.setBackgroundColor(ResourcesCompat.getColor(activity.resources, R.color.rooster_blue, null))
            true
        } catch (e: NullPointerException) {
            e.printStackTrace()
            false
        }
    }

    private fun setThemeNight(): Boolean {
        return try {
            activity.findViewById<View>(R.id.main_content)?.isSelected = true
            activity.findViewById<View>(R.id.toolbar)?.setBackgroundColor(ResourcesCompat.getColor(activity.resources, R.color.rooster_dark_blue, null))
            activity.findViewById<View>(R.id.tabs)?.setBackgroundColor(ResourcesCompat.getColor(activity.resources, R.color.rooster_dark_blue, null))
            true
        } catch (e: NullPointerException) {
            e.printStackTrace()
            false
        }
    }
}