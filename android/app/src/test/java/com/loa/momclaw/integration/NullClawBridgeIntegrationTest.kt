package com.loa.momclaw.integration

import android.content.Context
import com.loa.momclaw.domain.model.AgentConfig
import com.loa.momclaw.agent.NullClawBridge
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertFalse

/**
 * Integration tests for NullClaw Bridge lifecycle
 */
class NullClawBridgeIntegrationTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockFilesDir: File

    private lateinit var bridge: NullClawBridge

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.filesDir).thenReturn(mockFilesDir)
        whenever(mockFilesDir.absolutePath).thenReturn("/tmp/test")
        bridge = NullClawBridge(mockContext)
    }

    @Test
    fun testInitialState() {
        assertFalse(bridge.isRunning())
    }

    @Test
    fun testCannotStartWithoutSetup() {
        val result = bridge.start()
        assert(result.isFailure)
    }

    @Test
    fun testStopIsIdempotent() {
        bridge.stop()
        bridge.stop()
        assertFalse(bridge.isRunning())
    }
}
