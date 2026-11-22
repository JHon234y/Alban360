package com.example.data

import android.util.Log
import com.example.model.AiUiState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "CoffeeAiRepository"

class CoffeeAiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    fun streamAdvisory(prompt: String): Flow<AiUiState> = flow {
        emit(AiUiState.Loading)
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiUiState.Streaming(accumulated.toString()))
                    }
                }
            }
            if (accumulated.isNotBlank()) {
                emit(AiUiState.Success(accumulated.toString()))
            } else {
                emit(AiUiState.Error("El asistente no devolvió recomendaciones. Inténtalo nuevamente."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini stream timed out", e)
            emit(AiUiState.Error("La consulta tardó más de 60 segundos. Revisa tu conexión e inténtalo de nuevo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini inference failed", e)
            emit(AiUiState.Error(e.localizedMessage ?: "No se pudo completar el análisis con IA."))
        }
    }
}
