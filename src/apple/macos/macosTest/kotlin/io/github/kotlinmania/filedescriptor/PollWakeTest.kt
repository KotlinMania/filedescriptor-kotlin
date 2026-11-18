@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.libc.time.StdDuration
import kotlinx.cinterop.pointed
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import libc.cinterop.libc_get_descriptor_constants
import libc.cinterop.libc_pthread_kill
import libc.cinterop.libc_pthread_self
import libc.cinterop.libc_signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class PollWakeTest {
    private val constants get() = requireNotNull(libc_get_descriptor_constants()).pointed

    private fun withDelayedAction(action: () -> Unit, block: () -> Unit) =
        runBlocking {
            val job =
                launch(Dispatchers.Default) {
                    delay(40.milliseconds)
                    action()
                }
            try {
                block()
            } finally {
                job.join()
            }
        }

    @Test
    fun nullTimeoutBlocksUntilAnotherThreadWritesWakePipe() {
        Pipe.new().getOrThrow().use { pipe ->
            val start = TimeSource.Monotonic.markNow()
            withDelayedAction({ pipe.write.write(byteArrayOf(1)).getOrThrow() }) {
                val fd = Pollfd(pipe.read.asRawFileDescriptor(), POLLIN)
                assertEquals(1, poll(listOf(fd), null).getOrThrow())
                assertEquals(POLLIN, fd.revents)
                assertTrue(start.elapsedNow() >= 20.milliseconds)
            }
        }
    }

    @Test
    fun preservesInterruptedErrorForCrosstermRetry() {
        val target = libc_pthread_self()
        val previous = libc_signal(constants.sigusr1, staticCFunction { _: Int -> })
        try {
            Pipe.new().getOrThrow().use { pipe ->
                withDelayedAction({ assertEquals(0, libc_pthread_kill(target, constants.sigusr1)) }) {
                    val result = poll(listOf(Pollfd(pipe.read.asRawFileDescriptor(), POLLIN)), StdDuration.fromMillis(2000u))
                    val error = assertNotNull(result.error)
                    assertEquals(FileDescriptorErrorKind.Io, error.kind)
                    val cause = assertNotNull(error.systemError)
                    assertEquals(constants.eintr, cause.errorCode)
                    assertTrue(cause.interrupted)
                }
            }
        } finally {
            libc_signal(constants.sigusr1, previous)
        }
    }
}
