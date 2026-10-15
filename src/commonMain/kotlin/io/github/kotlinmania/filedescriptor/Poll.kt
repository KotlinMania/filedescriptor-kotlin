@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

import kotlin.time.Duration

/**
 * Examines a set of file descriptors for readiness.
 */
fun poll(pfd: List<Pollfd>, duration: Duration? = null): Result<Int> = pollPlatform(pfd, duration)

/**
 * Examines an array of file descriptors for readiness.
 */
fun poll(pfd: Array<Pollfd>, duration: Duration? = null): Result<Int> = pollPlatform(pfd.toList(), duration)
