@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.UnsafeNumber::class, kotlin.experimental.ExperimentalNativeApi::class)

package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.libc.time.StdDuration
import io.github.kotlinmania.libc.unix.fcntl
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.usePinned
import libc.cinterop.libc_fd_setsize
import libc.cinterop.libc_get_descriptor_constants
import kotlin.native.OsFamily
import kotlin.native.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import libc.cinterop.libc_close as close
import libc.cinterop.libc_pipe as pipe
import libc.cinterop.libc_read as read
import libc.cinterop.libc_write as write

class PollTest {
    private val constants get() = requireNotNull(libc_get_descriptor_constants()).pointed

    private class RawPipe : AutoCloseable {
        val reader: Int
        val writer: Int

        init {
            val pair =
                memScoped {
                    val fds = allocArray<IntVar>(2)
                    check(pipe(fds) == 0)
                    Pair(fds[0], fds[1])
                }
            reader = pair.first
            writer = pair.second
        }

        fun send() {
            byteArrayOf(42).usePinned { assertEquals(1L, write(writer, it.addressOf(0), 1u.convert())) }
        }

        fun receive() {
            ByteArray(1).usePinned { assertEquals(1L, read(reader, it.addressOf(0), 1u.convert())) }
        }

        override fun close() {
            close(reader)
            close(writer)
        }
    }

    @Test
    fun pollsCrosstermTerminalResizeAndWakeDescriptors() {
        RawPipe().use { terminal ->
            RawPipe().use { resize ->
                RawPipe().use { wake ->
                    val fds =
                        arrayOf(
                            Pollfd(terminal.reader.toLong(), POLLIN, POLLOUT),
                            Pollfd(resize.reader.toLong(), POLLIN, POLLOUT),
                            Pollfd(wake.reader.toLong(), POLLIN, POLLOUT),
                        )
                    assertEquals(0, poll(fds, StdDuration.fromMillis(0u)).getOrThrow())
                    assertTrue(fds.all { it.revents == 0.toShort() })
                    wake.send()
                    assertEquals(1, poll(fds, null).getOrThrow())
                    assertEquals(0.toShort(), fds[0].revents)
                    assertEquals(0.toShort(), fds[1].revents)
                    assertEquals(POLLIN, fds[2].revents)
                    wake.receive()
                    terminal.send()
                    resize.send()
                    assertEquals(2, poll(fds.toList(), StdDuration.fromMillis(50u)).getOrThrow())
                    assertEquals(POLLIN, fds[0].revents)
                    assertEquals(POLLIN, fds[1].revents)
                    assertEquals(0.toShort(), fds[2].revents)
                    assertTrue(fcntl(wake.reader, constants.f_getfd) >= 0)
                }
            }
        }
    }

    @Test
    fun waitsForFiniteTimeoutAndClearsReadiness() {
        RawPipe().use { raw ->
            val fds = listOf(Pollfd(raw.reader.toLong(), POLLIN, POLLIN))
            val start = TimeSource.Monotonic.markNow()
            assertEquals(0, poll(fds, StdDuration.fromMillis(30u)).getOrThrow())
            assertTrue(start.elapsedNow() >= 20.milliseconds)
            assertEquals(0.toShort(), fds.single().revents)
        }
    }

    @Test
    fun emptyDescriptorListHonorsTimeout() {
        val start = TimeSource.Monotonic.markNow()
        assertEquals(0, poll(emptyList(), StdDuration.fromMillis(30u)).getOrThrow())
        assertTrue(start.elapsedNow() >= 20.milliseconds)
    }

    @Test
    fun preservesHostPollingCount() {
        val pair = socketpair().getOrThrow()
        val first = pair.first
        val second = pair.second
        first.use { reader ->
            second.use { writer ->
                writer.write(byteArrayOf(7)).getOrThrow()
                val events = (POLLIN.toInt() or POLLOUT.toInt()).toShort()
                val fd = Pollfd(reader.asSocketDescriptor(), events)
                assertEquals(if (Platform.osFamily == OsFamily.MACOSX) 2 else 1, poll(listOf(fd), StdDuration.fromMillis(50u)).getOrThrow())
                assertEquals(events, fd.revents)
                val isDarwin = Platform.osFamily in setOf(OsFamily.MACOSX, OsFamily.IOS, OsFamily.WATCHOS, OsFamily.TVOS)
                assertEquals(
                    if (isDarwin) 1 else 2,
                    poll(listOf(Pollfd(fd.fd, POLLIN), Pollfd(fd.fd, POLLIN)), StdDuration.fromMillis(0u)).getOrThrow(),
                )
            }
        }
    }

    @Test
    fun largeFiniteTimeoutAndInfinityPreserveReadyDescriptors() {
        RawPipe().use { raw ->
            raw.send()
            val fds = listOf(Pollfd(raw.reader.toLong(), POLLIN))
            assertEquals(1, poll(fds, StdDuration.fromSecs(60uL * 24u * 60u * 60u)).getOrThrow())
            assertEquals(1, poll(fds, null).getOrThrow())
        }
    }

    @Test
    fun reportsClosedDescriptorWithoutLosingSystemError() {
        val fd = Pipe.new().getOrThrow().use { it.read.asRawFileDescriptor() }
        val item = Pollfd(fd, POLLIN)
        val result = poll(listOf(item), StdDuration.fromMillis(0u))
        if (Platform.osFamily == OsFamily.MACOSX) {
            val error = assertNotNull(result.error)
            assertEquals(FileDescriptorErrorKind.Io, error.kind)
            val cause = assertNotNull(error.systemError)
            assertEquals(constants.ebadf, cause.errorCode)
            assertEquals(false, cause.interrupted)
        } else {
            assertEquals(1, result.getOrThrow())
            assertTrue((item.revents.toInt() and POLLNVAL.toInt()) != 0)
        }
    }

    @Test
    fun validatesDescriptorWidthsAndSelectBounds() {
        assertEquals(FileDescriptorErrorKind.IllegalFdValue, poll(listOf(Pollfd(Long.MAX_VALUE, POLLIN)), StdDuration.fromMillis(0u)).error?.kind)
        if (Platform.osFamily == OsFamily.MACOSX) {
            assertEquals(FileDescriptorErrorKind.IllegalFdValue, poll(listOf(Pollfd(-1, POLLIN)), StdDuration.fromMillis(0u)).error?.kind)
            assertEquals(
                FileDescriptorErrorKind.FdValueOutsideFdSetSize,
                poll(listOf(Pollfd(libc_fd_setsize().toLong(), POLLIN)), StdDuration.fromMillis(0u)).error?.kind,
            )
        } else {
            val ignored = Pollfd(-1, POLLIN, POLLIN)
            assertEquals(0, poll(listOf(ignored), StdDuration.fromMillis(0u)).getOrThrow())
            assertEquals(0.toShort(), ignored.revents)
        }
    }

    @Test
    fun pipeAndDuplicateHaveIndependentKernelLifetimes() {
        Pipe.new().getOrThrow().use { pipe ->
            pipe.read.tryClone().getOrThrow().use { duplicate ->
                val raw = duplicate.asRawFileDescriptor().toInt()
                assertTrue((fcntl(raw, constants.f_getfd) and constants.fd_cloexec) != 0)
                pipe.read.close()
                pipe.write.write(byteArrayOf(10, 20, 30)).getOrThrow()
                val fds = listOf(Pollfd(duplicate.asRawFileDescriptor(), POLLIN))
                assertEquals(1, poll(fds, StdDuration.fromMillis(50u)).getOrThrow())
                val buffer = ByteArray(5)
                assertEquals(3, duplicate.read(buffer, 1, 3).getOrThrow())
                assertEquals(listOf<Byte>(0, 10, 20, 30, 0), buffer.toList())
            }
        }
    }
}
