package com.loa.momclaw.startup

import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * ServiceRegistry — Centralized service discovery and lifecycle management
 * 
 * Provides:
 * - Service registration and lookup by type/name
 * - Service health monitoring
 * - Thread-safe service state access
 * - Dependency-aware startup ordering
 */
object ServiceRegistry {
    
    private val services = ConcurrentHashMap<String, ServiceInfo>()
    private val serviceStates = ConcurrentHashMap<String, StateFlow<*>>()
    
    /**
     * Register a service with the registry
     */
    fun <T : Any> register(
        name: String,
        instance: T,
        stateFlow: StateFlow<*>? = null,
        dependencies: List<String> = emptyList()
    ) {
        val info = ServiceInfo(
            name = name,
            instance = instance,
            dependencies = dependencies,
            stateFlow = stateFlow
        )
        services[name] = info
        stateFlow?.let { serviceStates[name] = it }
        logger.info { "Registered service: $name with dependencies: $dependencies" }
    }
    
    /**
     * Unregister a service
     */
    fun unregister(name: String) {
        services.remove(name)
        serviceStates.remove(name)
        logger.info { "Unregistered service: $name" }
    }
    
    /**
     * Get a service instance by name
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getService(name: String): T? {
        return services[name]?.instance as? T
    }
    
    /**
     * Get a service instance by type
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getServiceByType(): T? {
        return services.values
            .firstOrNull { it.instance is T }
            ?.instance as? T
    }
    
    /**
     * Get service state flow
     */
    fun getServiceState(name: String): StateFlow<*>? {
        return serviceStates[name]
    }
    
    /**
     * Check if a service is registered
     */
    fun isRegistered(name: String): Boolean = services.containsKey(name)
    
    /**
     * Check if all dependencies are satisfied
     */
    fun areDependenciesMet(name: String): Boolean {
        val info = services[name] ?: return false
        return info.dependencies.all { depName ->
            val depInfo = services[depName]
            depInfo != null && isServiceHealthy(depName)
        }
    }
    
    /**
     * Check if a service is healthy
     */
    fun isServiceHealthy(name: String): Boolean {
        val state = serviceStates[name]?.value ?: return false
        return when (state) {
            is InferenceState.Running -> true
            is AgentState.Running -> true
            is StartupState.Running -> true
            else -> false
        }
    }
    
    /**
     * Get all registered service names
     */
    fun getRegisteredServices(): Set<String> = services.keys.toSet()
    
    /**
     * Get service info
     */
    fun getServiceInfo(name: String): ServiceInfo? = services[name]
    
    /**
     * Clear all registered services
     */
    fun clear() {
        services.clear()
        serviceStates.clear()
        logger.info { "ServiceRegistry cleared" }
    }
    
    /**
     * Get services sorted by dependency order
     */
    fun getStartupOrder(): List<String> {
        val visited = mutableSetOf<String>()
        val result = mutableListOf<String>()
        
        fun visit(name: String) {
            if (name in visited) return
            visited.add(name)
            
            services[name]?.dependencies?.forEach { dep ->
                visit(dep)
            }
            result.add(name)
        }
        
        services.keys.forEach { visit(it) }
        return result
    }
}

/**
 * Service information holder
 */
data class ServiceInfo(
    val name: String,
    val instance: Any,
    val dependencies: List<String>,
    val stateFlow: StateFlow<*>?
)
