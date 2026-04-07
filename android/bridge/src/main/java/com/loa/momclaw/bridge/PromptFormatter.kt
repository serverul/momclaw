package com.loa.momclaw.bridge

/**
 * Formats messages into a prompt string compatible with Gemma models.
 */
object PromptFormatter {
    
    /**
     * Formats a list of messages into Gemma's conversation format.
     * 
     * Gemma uses special tokens:
     * - <start_of_turn>user and <end_of_turn>
     * - <start_of_turn>model and <end_of_turn>
     * 
     * @param messages List of message DTOs
     * @return Formatted prompt string
     */
    fun formatPrompt(messages: List<MessageDto>): String {
        val builder = StringBuilder()
        
        for (msg in messages) {
            when (msg.role.lowercase()) {
                "system" -> {
                    // System prompt - prepend as instructions
                    builder.append("<start_of_turn>user\n")
                    builder.append("System instructions: ${msg.content}\n")
                    builder.append("<end_of_turn>\n")
                }
                "user" -> {
                    builder.append("<start_of_turn>user\n")
                    builder.append("${msg.content}\n")
                    builder.append("<end_of_turn>\n")
                }
                "assistant" -> {
                    builder.append("<start_of_turn>model\n")
                    builder.append("${msg.content}\n")
                    builder.append("<end_of_turn>\n")
                }
            }
        }
        
        // Add final model turn to prompt generation
        builder.append("<start_of_turn>model\n")
        
        return builder.toString()
    }

    /**
     * Alternative prompt format for older Gemma versions or different flavors.
     * Uses simpler token markers.
     */
    fun formatPromptSimple(messages: List<MessageDto>): String {
        val builder = StringBuilder()
        
        for (msg in messages) {
            when (msg.role.lowercase()) {
                "system" -> builder.append("SYSTEM: ${msg.content}\n\n")
                "user" -> builder.append("USER: ${msg.content}\n")
                "assistant" -> builder.append("ASSISTANT: ${msg.content}\n")
            }
        }
        
        builder.append("ASSISTANT:")
        return builder.toString()
    }

    /**
     * Cleans response text by removing special tokens and formatting artifacts.
     */
    fun cleanResponse(response: String): String {
        return response
            .replace("<end_of_turn>", "")
            .replace("<start_of_turn>", "")
            .trim()
    }
}
