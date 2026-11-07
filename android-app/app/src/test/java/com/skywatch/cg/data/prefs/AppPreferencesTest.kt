package com.skywatch.cg.data.prefs

import android.content.Context
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AppPreferencesTest {
    private lateinit var context: Context

    @Before
    fun clearPreferences() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("sky_watch_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun restorePreferences() {
        context.getSharedPreferences("sky_watch_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun radarWatchIsEnabledByDefaultAndPersistsChanges() {
        val preferences = AppPreferences.getInstance(context)

        assertTrue(preferences.radarWatchEnabled)
        preferences.radarWatchEnabled = false

        assertFalse(AppPreferences.getInstance(context).radarWatchEnabled)
    }
}
