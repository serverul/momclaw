package com.loa.momclaw.config

/**
 * Centralized network configuration for MomClaw.
 *
 * All URLs and network-related constants are defined here
 * to avoid scattering hardcoded values across the codebase.
 */
object NetworkConfig {

    // LiteRT Bridge (inference server)
    const val DEFAULT_BASE_URL = "http://localhost:8080"
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8080"

    // NullClaw Agent server
    const val AGENT_URL = "http://localhost:9090"

    // HuggingFace API
    const val HUGGINGFACE_API_URL = "https://huggingface.co/api/"
    const val HUGGINGFACE_FILES_URL = "https://huggingface.co/"
    const val DEFAULT_MODEL_REPO = "litert-community/gemma-4-E4B-it-litertlm"
}
