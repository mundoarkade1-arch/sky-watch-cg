package com.skywatch.cg.util

object AppVersionComparator {
    fun compare(candidate: String, installed: String): Int? {
        val candidateParts = parse(candidate) ?: return null
        val installedParts = parse(installed) ?: return null

        for (index in 0 until maxOf(candidateParts.size, installedParts.size)) {
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            val installedPart = installedParts.getOrElse(index) { 0 }
            if (candidatePart != installedPart) {
                return candidatePart.compareTo(installedPart)
            }
        }
        return 0
    }

    private fun parse(version: String): List<Int>? {
        val normalized = version.removePrefix("v").removePrefix("V")
        val parts = normalized.split(".")
        if (parts.isEmpty() || parts.any { part -> part.isEmpty() || part.any { !it.isDigit() } }) {
            return null
        }
        return parts.map { it.toIntOrNull() ?: return null }
    }
}
