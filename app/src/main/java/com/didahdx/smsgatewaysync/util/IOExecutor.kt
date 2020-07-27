package com.didahdx.smsgatewaysync.util

import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class IOExecutor : Executor {
    override fun execute(runnable: Runnable) {
        Executors.newSingleThreadExecutor().execute(runnable)
    }

    companion object {
        private var executor: IOExecutor? = null
        private val LOCK = Any()
        val instance: IOExecutor?
            get() {
                if (executor == null) {
                    synchronized(
                        LOCK
                    ) { executor = IOExecutor() }
                }
                return executor
            }
    }
}