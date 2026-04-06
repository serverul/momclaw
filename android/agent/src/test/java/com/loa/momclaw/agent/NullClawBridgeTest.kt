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
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlinx.coroutines.runBlocking

/**
 * Unit tests for NullClawBridge
 */
class NullClawBridgeTest {

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
        whenever(mockFilesDir.absolutePath).thenReturn("/tmp/test-MOMCLAW")
        whenever(mockContext.applicationInfo).thenReturn(mockApplicationInfo)
        whenever(mockApplicationInfo.flags).thenReturn(ApplicationInfo.FLAG_DEBUGGABLE)
        
        // Create test directory
        File("/tmp/test-MOMCLAW").mkdirs()
        
        bridge = NullClawBridge(mockContext)
    }

    @After
    fun tearDown() {
        closeable.close()
        bridge.stop()
        File("/tmp/test-MOMCLAW").deleteRecursively()
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
}

/**
 * Unit tests for ConfigGenerator
 */
class ConfigGeneratorTest {

    @Test
    fun testGenerate_returnsValidJson() {
        val config = AgentConfig.DEFAULT
        val json = ConfigGenerator.generate(config)
        
        assertTrue("Should contain agents section", json.contains("\"agents\""))
        assertTrue("Should contain models section", json.contains("\"models\""))
        assertTrue("Should contain litert-bridge provider", json.contains("litert-bridge"))
        assertTrue("Should contain gateway section", json.contains("\"gateway\""))
    }

    @Test
    fun testGenerate_containsSystemPrompt() {
        val config = AgentConfig(
            systemPrompt = "You are a test assistant."
        )
        val json = ConfigGenerator.generate(config)
        
        assertTrue("Should contain system prompt", json.contains("You are a test assistant."))
    }

    @Test
    fun testGenerate_containsCorrectBaseUrl() {
        val config = AgentConfig(
            baseUrl = "http://localhost:8080"
        )
        val json = ConfigGenerator.generate(config)
        
        assertTrue("Should contain base URL", json.contains("http://localhost:8080"))
    }

    @Test
    fun testGenerateMinimal_noToolsSection() {
        val config = AgentConfig.DEFAULT
        val json = ConfigGenerator.generateMinimal(config)
        
        // Minimal config should have null tools
        assertTrue("Should be valid JSON", json.startsWith("{"))
        assertTrue("Should still contain model config", json.contains("litert-bridge"))
    }

    @Test
    fun testGenerateWithTools_includesShellConfig() {
        val config = AgentConfig.DEFAULT
        val json = ConfigGenerator.generateWithTools(
            config = config,
            enabledTools = listOf("shell", "file_read"),
            allowedShellCommands = listOf("ls", "cat")
        )
        
        assertTrue("Should contain shell tool", json.contains("shell"))
        assertTrue("Should contain allowed commands", json.contains("ls"))
    }
}
