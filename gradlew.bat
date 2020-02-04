/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("unused")

package kotlinx.coroutines

import kotlinx.coroutines.internal.*
import kotlinx.coroutines.scheduling.*
import java.util.*
import kotlin.coroutines.*

/**
 * Name of the property that defines the maximal number of threads that are used by [Dispatchers.IO] coroutines dispatcher.
 */
public const val IO_PARALLELISM_PROPERTY_NAME = "kotlinx.coroutines.io.parallelism"

/**
 * Groups various implementations of [CoroutineDispatcher].
 */
public actual object Dispatchers {
    /**
     * The default [CoroutineDispatcher] that is used by all standard builders like
     * [launch][CoroutineScope.launch], [async][CoroutineScope.async], etc
     * if no dispatcher nor any other [ContinuationInterceptor] is specified in their context.
     *
     * It is backed by a shared pool of threads on JVM. By default, the maximal level of parallelism used
     * by this dispatcher is equal to the number of CPU cores, but is at least two.
     * Level of parallelism X guarantees that no more than X tasks can be executed in this dispatcher in parallel.
     */
    @JvmStatic
    public actual val Default: CoroutineDispatcher = createDefaultDispatcher()

    /**
     * A coroutine dispatcher that is confined to the Main thread operating with UI objects.
     * This dispatcher can be used either directly or via [MainScope] factory.
     * Usually such dispatcher is single-threaded.
     *
     * Access to this property may throw [IllegalStateException] if no main thread dispatchers are present in the classpath.
     *
     * Depending on platform and classpath it can be mapped to different dispatchers:
     * - On JS and Native it is equivalent of [Default] dispatcher.
     * - On JVM it either Android main thread dispatcher, JavaFx or Swing EDT dispatcher. It is chosen by
     *   [`ServiceLoader`](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
     *
     * In order to work with `Main` dispatcher, following artifact should be added to project runtime dependencies:
     *  - `kotlinx-coroutines-android` for Android Main thread dispatcher
     *  - `kotlinx-coroutines-javafx` for JavaFx