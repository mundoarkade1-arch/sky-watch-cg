package com.skywatch.cg.data.radar

class RadarRepository(
    private val remoteSource: RadarRemoteSource = RadarRemoteSource()
) {
    suspend fun refresh(): RadarSnapshot = remoteSource.fetchSnapshot()
}
