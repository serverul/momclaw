package com.loa.momclaw.agent

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.AssetManager
import com.loa.momclaw.agent.model.AgentConfig
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Unit tests for NullClaw Agent integration
 * Tests the agent configuration and basic functionality
 */
class NullClawAgentTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockApplicationInfo: ApplicationInfo

    @Mock
    private lateinit var mockAssetManager: AssetManager

    @Mock
    private lateinit var mockFilesDir: File

    private lateinit var bridge: NullClawBridge
    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        
        // Setup mock files directory
        whenever(mockContext.filesDir).thenReturn(mockFilesDir)
        whenever(mockFilesDir.absolutePath).thenReturn("/tmp/test-momclaw-agent")
        whenever(mockContext.applicationInfo).thenReturn(mockApplicationInfo)
        whenever(mockApplicationInfo.flags).thenReturn(ApplicationInfo.FLAG_DEBUGGABLE)
        whenever(mockContext.assets).thenReturn(mockAssetManager)
        
        // Create test directory
        File("/tmp/test-momclaw-agent").mkdirs()
        
        bridge = NullClawBridge(mockContext)
    }

    @After
    fun tearDown() {
        closeable.close()
        bridge.stop()
        File("/tmp/test-momclaw-agent").deleteRecursively()
    }

    @Test
    fun testInitialState_isNotRunning() {
        assertFalse("Bridge should not be running initially", bridge.isRunning())
    }

    @Test
    fun testCannotStartWithoutSetup(): Unit = runBlocking {
        val result = bridge.start()
        assertTrue("Start should fail without setup", result.isFailure)
        assertTrue("Error should mention setup", 
            result.exceptionOrNull()?.message?.contains("not set up") == true)
    }

    @Test
    fun testStopIsIdempotent() {
        // Should not throw
        bridge.stop()
        bridge.stop()
        assertFalse(bridge.isRunning())
    }

    @Test
    fun testGetEndpoint_returnsLocalhostPort() {
        assertEquals("http://localhost:9090", bridge.getEndpoint())
    }

    @Test
    fun testGetPid_returnsNullWhenNotRunning() {
        assertNull(bridge.getPid())
    }

    @Test
    fun testHealthCheck_failsWhenNotRunning(): Unit = runBlocking {
        val isHealthy = bridge.checkHealth()
        assertFalse("Health check should fail when not running", isHealthy)
    }

    @Test
    fun testAgentConfig_defaultValues() {
        val config = AgentConfig.DEFAULT
        
        assertEquals("http://localhost:8080", config.baseUrl)
        assertNotNull(config.systemPrompt)
        assertEquals(0.7f, config.temperature)
        assertEquals(2048, config.maxTokens)
        assertEquals("sqlite", config.memoryBackend)
    }

    @Test
    fun testAgentConfig_customValues() {
        val config = AgentConfig(
            systemPrompt = "Custom prompt",
            temperature = 0.5f,
            maxTokens = 1024,
            modelPrimary = "custom-model",
            modelPath = "/custom/path/model.bin",
            baseUrl = "http://custom:8080",
            memoryBackend = "memory",
            memoryPath = "/custom/path/memory.db"
        )
        
        assertEquals("Custom prompt", config.systemPrompt)
        assertEquals(0.5f, config.temperature)
        assertEquals(1024, config.maxTokens)
        assertEquals("http://custom:8080", config.baseUrl)
        assertEquals("memory", config.memoryBackend)
    }
}
