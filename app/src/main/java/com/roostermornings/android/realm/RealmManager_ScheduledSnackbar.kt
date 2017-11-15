package com.roostermornings.android.realm

import android.app.Activity
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.util.SnackbarManager
import io.realm.Realm
import io.realm.RealmObject
import java.lang.reflect.Modifier
import java.util.*
import javax.inject.Inject

/**
 * Created by bscholtz on 2017/11/15.
 */
class RealmManager_ScheduledSnackbar {
    @Inject lateinit var realm: Realm

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
    }

    fun closeRealm() {
        realm.close()
    }

    // Note: this will delete scheduled snackbars once fetched
    fun getScheduledSnackbarsForActivity(activity: Activity): ArrayList<ScheduledSnackbar> {
        val gson = GsonBuilder().create()
        val snackbarsForActivity = ArrayList<ScheduledSnackbar>()
        val calendar = Calendar.getInstance()

        realm.where(ScheduledSnackbar::class.java)
                .equalTo("localDisplayClassName", activity.localClassName)
                .findAll().toList()
                .filter { (it.displayTime == -1L) || (it.displayTime < calendar.timeInMillis) }
                .onEach { snackbar ->

            val unmanagedScheduledSnackbar = realm.copyFromRealm(snackbar)

            val type = object : TypeToken<SnackbarManager.Companion.SnackbarQueueElement>() {}.type
            gson.fromJson<SnackbarManager.Companion.SnackbarQueueElement>(unmanagedScheduledSnackbar.jsonSnackbarQueueElement, type)?.let {
                unmanagedScheduledSnackbar.snackbarQueueElement = it
            }
            snackbarsForActivity.add(unmanagedScheduledSnackbar)

            realm.executeTransaction {
                snackbar.deleteFromRealm()
            }
        }.takeIf { it.isNotEmpty() }?.also {
            return snackbarsForActivity
        }
        return snackbarsForActivity
    }

    fun updateOrCreateScheduledSnackbarEntry(snackbarQueueElement: SnackbarManager.Companion.SnackbarQueueElement, localDisplayClassName: String, displayTimeMillis: Long) {
        // Ignore serialization of RealmObject subclass to avoid StackOverFlow error
        val GSON = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .excludeFieldsWithModifiers(Modifier.ABSTRACT)
                .addSerializationExclusionStrategy(object : ExclusionStrategy {
                    override fun shouldSkipField(f: FieldAttributes): Boolean {
                        return f.declaringClass == RealmObject::class.java
                    }

                    override fun shouldSkipClass(clazz: Class<*>): Boolean {
                        return false
                    }
                }).create()

        val scheduledSnackbar = ScheduledSnackbar()
        scheduledSnackbar.jsonSnackbarQueueElement = GSON.toJson(snackbarQueueElement)
        scheduledSnackbar.localDisplayClassName = localDisplayClassName
        scheduledSnackbar.displayTime = displayTimeMillis

        // Insert a new entry, or update if it exists.
        // Working with an unmanaged Realm object, you're only guaranteed of no duplication if
        // primary key present.
        realm.executeTransaction {
            realm.insertOrUpdate(scheduledSnackbar)
        }
    }
}