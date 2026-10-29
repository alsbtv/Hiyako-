package com.ejemplo.hiyako.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object RateLimiter {
    private val anilistMutex = Mutex()
    private val jikanMutex = Mutex()
    private val translateMutex = Mutex()

    private var lastAnilistCall = 0L
    private var lastJikanCall = 0L
    private var lastTranslateCall = 0L

    suspend fun throttleAnilist() {
        anilistMutex.withLock {
            val now = System.currentTimeMillis()
            val diff = now - lastAnilistCall
            if (diff < 2000) {
                delay(2000 - diff)
            }
            lastAnilistCall = System.currentTimeMillis()
        }
    }

    suspend fun throttleJikan() {
        jikanMutex.withLock {
            val now = System.currentTimeMillis()
            val diff = now - lastJikanCall
            if (diff < 400) {
                delay(400 - diff)
            }
            lastJikanCall = System.currentTimeMillis()
        }
    }

    suspend fun throttleTranslate() {
        translateMutex.withLock {
            val now = System.currentTimeMillis()
            val diff = now - lastTranslateCall
            if (diff < 1000) {
                delay(1000 - diff)
            }
            lastTranslateCall = System.currentTimeMillis()
        }
    }
}
