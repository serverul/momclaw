package com.loa.momclaw.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * Accessibility utilities for Material3 compliance.
 * Provides modifier extensions for screen reader support and WCAG guidelines.
 */
object AccessibilityUtils {

    /**
     * Adds content description for screen readers
     */
    fun Modifier.accessibilityLabel(label: String): Modifier =
        semantics { contentDescription = label }

    /**
     * Marks component as a button with role
     */
    fun Modifier.accessibilityButton(
        label: String,
        actionDescription: String = "Tap to $label"
    ): Modifier =
        semantics {
            role = Role.Button
            contentDescription = actionDescription
        }

    /**
     * Marks component as a toggle with state
     */
    fun Modifier.accessibilityToggle(
        label: String,
        isChecked: Boolean
    ): Modifier =
        semantics {
            role = Role.Switch
            stateDescription = if (isChecked) "On" else "Off"
            contentDescription = "$label, currently ${if (isChecked) "on" else "off"}"
        }

    /**
     * Marks as heading for screen reader navigation
     */
    fun Modifier.accessibilityHeading(): Modifier =
        semantics { heading() }

    /**
     * Hides decorative element from accessibility tree
     */
    fun Modifier.accessibilityInvisible(): Modifier =
        clearAndSetSemantics { }

    /**
     * Marks live region for dynamic content updates
     * Use Polite for non-urgent, Assertive for urgent announcements
     */
    fun Modifier.accessibilityLiveRegion(polite: Boolean = true): Modifier =
        semantics {
            liveRegion = if (polite) {
                androidx.compose.ui.semantics.LiveRegionMode.Polite
            } else {
                androidx.compose.ui.semantics.LiveRegionMode.Assertive
            }
        }
}
