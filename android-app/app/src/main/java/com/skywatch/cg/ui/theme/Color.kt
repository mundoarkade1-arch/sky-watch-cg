package com.skywatch.cg.ui.theme

import androidx.compose.ui.graphics.Color

// Airport panel dark theme colors
object SkyWatchColors {
    // Background & surfaces
    val Background = Color(0xFF0A0F1C)
    val Surface = Color(0xFF121A2B)
    val SurfaceContainer = Color(0xFF1C2842)
    val Border = Color(0xFF1E2A42)

    // Text
    val OnBackground = Color(0xFFF1F5F9)
    val OnSurface = Color(0xFFF1F5F9)
    val OnSurfaceVariant = Color(0xFF8A97B1)
    val OnSurfaceLow = Color(0xFF8A97B1)

    // Accents
    val ArrivalAccent = Color(0xFF38BDF8)   // Sky blue for arrivals
    val DepartureAccent = Color(0xFFFBBF24) // Amber for departures
    val ArrivalAccentContainer = Color(0xFF06223A) // Dark blue for selected arrival chip text
    val DepartureAccentContainer = Color(0xFF3A2A00) // Dark amber for selected departure chip

    // Next flight card gradient
    val NextFlightGradientStart = Color(0xFF0F3A5C)
    val NextFlightGradientEnd = Color(0xFF12264A)
    val NextFlightBorder = Color(0xFF1F4E78)

    // Status
    val Error = Color(0xFFEF4444)
    val ErrorContainer = Color(0xFF3F1717)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)

    // Placeholder/skeleton
    val Placeholder = Color(0xFF1E2A42)
    val PlaceholderHighlight = Color(0xFF2A3A5A)
}