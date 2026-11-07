package com.skywatch.cg.data.radar

fun radarAlertHeadline(alertCount: Int): String =
    if (alertCount > 1) "VOO DETECTADO · +${alertCount - 1}" else "VOO DETECTADO"
