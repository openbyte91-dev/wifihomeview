package com.openbyt91dev.wifihomeview.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `defaults enable alerts and deep scanning`() = runTest {
        val repository = createRepository()

        val settings = repository.settings.first()

        assertTrue(settings.backgroundAlertsEnabled)
        assertEquals(ScanIntensity.DEEP, settings.scanIntensity)
    }

    @Test
    fun `persists alert toggle and scan intensity`() = runTest {
        val repository = createRepository()

        repository.setBackgroundAlertsEnabled(false)
        repository.setScanIntensity(ScanIntensity.QUICK)

        val settings = repository.settings.first()
        assertFalse(settings.backgroundAlertsEnabled)
        assertEquals(ScanIntensity.QUICK, settings.scanIntensity)
    }

    private fun TestScope.createRepository(): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { temporaryFolder.newFile("settings.preferences_pb") }
        )
        return SettingsRepository(dataStore)
    }
}
