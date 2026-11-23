package com.skywatch.cg.data.radar

import kotlin.math.abs

fun normalizeAzimuthDegrees(value: Float): Float = ((value % 360f) + 360f) % 360f

fun shortestAzimuthDeltaDegrees(from: Float, to: Float): Float {
    val delta = normalizeAzimuthDegrees(to) - normalizeAzimuthDegrees(from)
    return when {
        delta > 180f -> delta - 360f
        delta < -180f -> delta + 360f
        else -> delta
    }
}

fun smoothAzimuthDegrees(previous: Float?, current: Float, factor: Float = 0.2f): Float {
    val normalized = normalizeAzimuthDegrees(current)
    if (previous == null || factor >= 1f) return normalized
    if (factor <= 0f) return normalizeAzimuthDegrees(previous)
    return normalizeAzimuthDegrees(previous + shortestAzimuthDeltaDegrees(previous, normalized) * factor)
}

fun hasRotationVectorSensor(sensorAvailable: Boolean): Boolean = sensorAvailable
