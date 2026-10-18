package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.libc.time.StdDuration

/** Examines a set of file descriptors for readiness. */
fun poll(pfd: List<Pollfd>, duration: StdDuration? = null): PollResult =
    PollResult.from(pollPlatform(pfd, duration))

/** Examines an array of file descriptors for readiness. */
fun poll(pfd: Array<Pollfd>, duration: StdDuration? = null): PollResult =
    PollResult.from(pollPlatform(pfd.toList(), duration))
