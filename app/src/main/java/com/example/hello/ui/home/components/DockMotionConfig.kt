package com.example.hello.ui.home.components

object DockMotionConfig {
    // High stiffness (1500f) produces snappy, instant (<80ms) spring movement with zero delay
    const val PILL_SPRING_STIFFNESS = 1500f
    // Gentle bounce damping for organic tactile feel without sluggish oscillation
    const val PILL_SPRING_DAMPING = 0.82f
    // Fast snappy screen crossfade (80ms) eliminates transition lag
    const val SCREEN_FADE_DURATION_MS = 80
    // Disable default click indication / ripple background on tabs ("only the pill")
    const val HAS_TAB_CLICK_INDICATION = false
}
