package com.example.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    fun tryEmit(event: AppEvent): Boolean {
        return _events.tryEmit(event)
    }

    suspend fun emit(event: AppEvent) {
        _events.emit(event)
    }
}
