package com.example.hello

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.preferences.DataStoreUserPreferencesRepository
import com.example.hello.data.preferences.helloDataStore
import com.example.hello.data.repository.CaptureFlowRepository
import com.example.hello.data.repository.ContactRepository
import com.example.hello.platform.avatar.AvatarBitmapCache
import com.example.hello.platform.location.LocationService
import com.example.hello.platform.notification.FollowUpAlarmScheduler

/**
 * Lean process-wide Application container holding lazy singletons so Activities and
 * Composables share one Room database, repository graph, preferences repository, and
 * LocationService instead of recreating and leaking them per launch or configuration change.
 */
class HelloApplication : Application() {

    val database: HelloDatabase by lazy {
        HelloDatabase.getInstance(applicationContext)
    }

    val alarmScheduler: FollowUpAlarmScheduler by lazy {
        FollowUpAlarmScheduler(applicationContext)
    }

    val contactRepository: ContactRepository by lazy {
        ContactRepository(
            database = database,
            alarmScheduler = alarmScheduler
        )
    }

    val captureFlowRepository: CaptureFlowRepository by lazy {
        CaptureFlowRepository(database = database)
    }

    val userPreferencesRepository: DataStoreUserPreferencesRepository by lazy {
        DataStoreUserPreferencesRepository(applicationContext.helloDataStore)
    }

    val locationService: LocationService by lazy {
        LocationService(applicationContext)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            AvatarBitmapCache.trimMemory()
        }
    }

    companion object {
        fun from(context: Context): HelloApplication? =
            context.applicationContext as? HelloApplication
    }
}
