package com.skywatch.cg.data.radar

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Laço de vigilância. O chamador deve iniciar/cancelar este laço junto ao ciclo
 * de vida STARTED; enquanto cancelado, nenhuma consulta é feita.
 */
class RadarSurveillance(
    private val repository: RadarRepository,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val wait: suspend (Long) -> Unit = { delay(it) }
) {
    suspend fun run(
        isStarted: () -> Boolean,
        isEnabled: () -> Boolean,
        onSnapshot: suspend (RadarSnapshot, Long) -> Unit,
        onError: suspend (Throwable) -> Unit = {}
    ) {
        require(intervalMillis > 0L)
        while (currentCoroutineContext().isActive) {
            if (isStarted() && isEnabled()) {
                try {
                    onSnapshot(repository.refresh(), clockMillis())
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    onError(error)
                }
            }
            wait(intervalMillis)
        }
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 10_000L
    }
}
