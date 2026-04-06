package com.loa.momclaw.startup

import android.content.Context
import com.loa.momclaw.domain.model.AgentConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for StartupManager
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StartupManagerTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var startupManager: StartupManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        startupManager = StartupManager(mockContext)
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(StartupState.Idle, StartupManager.state.value)
    }

    @Test
    fun testDefaultConfigIsValid() {
        val config = AgentConfig.DEFAULT
        assertFalse(config.systemPrompt.isEmpty())
        assertTrue(config.temperature in 0.0f..2.0f)
        assertTrue(config.maxTokens > 0)
    }

    @Test
    fun testServicesNotRunningInitially() {
        assertFalse(startupManager.areServicesRunning())
    }
}
