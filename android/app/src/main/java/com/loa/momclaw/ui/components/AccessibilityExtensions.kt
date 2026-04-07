package com.loa.momclaw.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*

/**
 * Accessibility extensions for UI components.
 */

/**
 * Sets accessibility properties for a chat message.
 */
fun Modifier.chatMessageAccessibility(
    isUser: Boolean,
    content: String,
    timestamp: String,
    isStreaming: Boolean = false
): Modifier {
    return this.semantics {
        contentDescription = buildString {
            append(if (isUser) "You" else "Assistant")
            append(": ")
            append(content)
            if (isStreaming) {
                append(". Streaming...")
            }
            append(". Sent at $timestamp")
        }
        role = Role.Button
        stateDescription = if (isStreaming) "Streaming" else "Delivered"
    }
}

/**
 * Sets accessibility properties for a model card.
 */
fun Modifier.modelCardAccessibility(
    modelName: String,
    status: String,
    size: String
): Modifier {
    return this.semantics {
        contentDescription = "$modelName. $status. Size: $size"
        role = Role.Button
    }
}

/**
 * Sets accessibility properties for a button.
 */
fun Modifier.buttonAccessibility(
    action: String,
    isEnabled: Boolean
): Modifier {
    return this.semantics {
        contentDescription = action
        role = Role.Button
        stateDescription = if (isEnabled) "Enabled" else "Disabled"
    }
}

/**
 * Sets accessibility properties for an input field.
 */
fun Modifier.inputFieldAccessibility(
    label: String,
    currentValue: String,
    isRequired: Boolean = false
): Modifier {
    return this.semantics {
        contentDescription = buildString {
            append(label)
            append(" input field")
            if (currentValue.isNotEmpty()) {
                append(". Current value: $currentValue")
            }
            if (isRequired) {
                append(". Required")
            }
        }
        role = Role.TextBox
    }
}

/**
 * Sets accessibility properties for a slider.
 */
fun Modifier.sliderAccessibility(
    label: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>
): Modifier {
    return this.semantics {
        contentDescription = "$label slider. Current value: ${String.format("%.1f", currentValue)}. Range: ${String.format("%.1f", range.start)} to ${String.format("%.1f", range.endInclusive)}"
        role = Role.Slider
    }
}

/**
 * Sets accessibility properties for a switch.
 */
fun Modifier.switchAccessibility(
    label: String,
    isOn: Boolean
): Modifier {
    return this.semantics {
        contentDescription = "$label switch"
        role = Role.Switch
        stateDescription = if (isOn) "On" else "Off"
    }
}

/**
 * Sets accessibility properties for navigation items.
 */
fun Modifier.navigationItemAccessibility(
    label: String,
    isSelected: Boolean
): Modifier {
    return this.semantics {
        contentDescription = "$label tab"
        role = Role.Tab
        stateDescription = if (isSelected) "Selected" else "Not selected"
    }
}
