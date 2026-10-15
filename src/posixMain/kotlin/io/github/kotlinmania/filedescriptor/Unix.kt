// port-lint: source unix.rs
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.UnsafeNumber::class, kotlin.experimental.ExperimentalNativeApi::class)

package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.libc.unix.fcntl
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import libc.cinterop.libc_fd_isset
import libc.cinterop.libc_fd_set
import libc.cinterop.libc_fd_set_alloc
import libc.cinterop.libc_fd_set_free
import libc.cinterop.libc_fd_setsize
import libc.cinterop.libc_get_descriptor_constants
import libc.cinterop.libc_get_errno
import libc.cinterop.libc_ioctl
import libc.cinterop.libc_select_timeout
import libc.cinterop.libc_strerror
import kotlin.native.OsFamily
import kotlin.native.Platform
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import io.github.kotlinmania.libc.unix.Pollfd as LibcPollfd
import io.github.kotlinmania.libc.unix.poll as libcPoll
import libc.cinterop.libc_close as systemClose
import libc.cinterop.libc_dup as systemDup
import libc.cinterop.libc_dup2 as systemDup2
import libc.cinterop.libc_read as systemRead
import libc.cinterop.libc_socketpair as systemSocketpair
import libc.cinterop.libc_write as systemWrite

internal val constants get() = requireNotNull(libc_get_descriptor_constants()).pointed

internal fun systemError(): SystemIoException {
    val code = libc_get_errno()
    return SystemIoException(code, code == constants.eintr, libc_strerror(code)?.toKString() ?: "System I/O error $code")
}

private fun checkedFd(fd: RawFileDescriptor): Int {
    if (fd < 0 || fd > Int.MAX_VALUE) throw Error.IllegalFdValue(fd)
    return fd.toInt()
}

actual fun cloexec(fd: RawFileDescriptor): Result<Unit> = changeCloseOnExec(fd, true)

actual fun noCloexec(fd: RawFileDescriptor): Result<Unit> = changeCloseOnExec(fd, false)

private fun changeCloseOnExec(fd: RawFileDescriptor, enabled: Boolean): Result<Unit> =
    runCatching {
        val raw = checkedFd(fd)
        val flags = fcntl(raw, constants.f_getfd)
        if (flags == -1) throw Error.Fcntl(systemError())
        val updated = if (enabled) flags or constants.fd_cloexec else flags and constants.fd_cloexec.inv()
        if (fcntl(raw, constants.f_setfd, updated) == -1) throw Error.Cloexec(systemError())
    }

actual fun nonAtomicDup(fd: RawFileDescriptor): Result<OwnedHandle> =
    runCatching {
        val duplicate = systemDup(checkedFd(fd))
        if (duplicate == -1) throw Error.Dup(fd, systemError())
        ownDuplicate(duplicate)
    }

actual fun nonAtomicDup2(fd: RawFileDescriptor, destFd: RawFileDescriptor): Result<OwnedHandle> =
    runCatching {
        val duplicate = systemDup2(checkedFd(fd), checkedFd(destFd))
        if (duplicate == -1) throw Error.Dup2(fd, destFd, systemError())
        ownDuplicate(duplicate)
    }

private fun ownDuplicate(fd: Int): OwnedHandle {
    val owned = OwnedHandle(fd.toLong())
    try {
        cloexec(fd.toLong()).getOrThrow()
        return owned
    } catch (error: Throwable) {
        owned.close()
        throw error
    }
}

actual fun dupImpl(fd: AsRawFileDescriptor, handleType: HandleType): Result<OwnedHandle> =
    runCatching {
        val raw = fd.asRawFileDescriptor()
        val duplicate = fcntl(checkedFd(raw), constants.f_dupfd_cloexec, 0)
        if (duplicate == -1) {
            val error = systemError()
            if (error.errorCode == constants.einval) {
                nonAtomicDup(raw).getOrThrow()
            } else {
                throw Error.Dup(raw, error)
            }
        } else {
            OwnedHandle(duplicate.toLong(), handleType)
        }
    }

actual fun dup2Impl(fd: AsRawFileDescriptor, destFd: RawFileDescriptor): Result<OwnedHandle> =
    runCatching { duplicatePosix(checkedFd(fd.asRawFileDescriptor()), checkedFd(destFd)).getOrThrow() }

internal expect fun duplicatePosix(fd: Int, destFd: Int): Result<OwnedHandle>

internal expect fun createRawPipe(fds: CPointer<IntVar>): Int

internal expect val atomicCloseOnExec: Boolean

actual fun probeHandleTypePlatform(handle: RawFileDescriptor): HandleType = HandleType.Unknown

actual fun closeHandlePlatform(handle: RawFileDescriptor, handleType: HandleType) {
    systemClose(checkedFd(handle))
}

actual fun asStdioImpl(fd: FileDescriptor): Result<Any?> = fd.tryClone()

actual fun asFileImpl(fd: FileDescriptor): Result<Any?> = fd.tryClone()

actual fun setNonBlockingImpl(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> =
    runCatching {
        val raw = checkedFd(fd.asRawFileDescriptor())
        memScoped {
            val enabled = alloc<IntVar>()
            enabled.value = if (nonBlocking) 1 else 0
            if (libc_ioctl(raw, constants.fionbio, enabled.ptr) != 0) throw Error.FionBio(systemError())
        }
    }

actual fun redirectStdioImpl(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> =
    runCatching {
        val destination =
            when (stdio) {
                StdioDescriptor.Stdin -> 0L
                StdioDescriptor.Stdout -> 1L
                StdioDescriptor.Stderr -> 2L
            }
        val original = FileDescriptor(dupImpl(OwnedHandle(destination), HandleType.Unknown).getOrThrow())
        try {
            dup2Impl(f, destination).getOrThrow().intoRawFileDescriptor()
            noCloexec(destination).getOrThrow()
            original
        } catch (error: Throwable) {
            original.close()
            throw error
        }
    }

actual fun readPlatform(
    fd: RawFileDescriptor,
    handleType: HandleType,
    buf: ByteArray,
    offset: Int,
    length: Int,
): Result<Int> =
    runCatching {
        require(offset >= 0 && length >= 0 && offset <= buf.size - length)
        val raw = checkedFd(fd)
        val size = buf.usePinned { systemRead(raw, if (length == 0) null else it.addressOf(offset), length.convert()) }
        if (size < 0) throw Error.Io(systemError())
        size.toInt()
    }

actual fun writePlatform(
    fd: RawFileDescriptor,
    handleType: HandleType,
    buf: ByteArray,
    offset: Int,
    length: Int,
): Result<Int> =
    runCatching {
        require(offset >= 0 && length >= 0 && offset <= buf.size - length)
        val raw = checkedFd(fd)
        val size = buf.usePinned { systemWrite(raw, if (length == 0) null else it.addressOf(offset), length.convert()) }
        if (size < 0) throw Error.Io(systemError())
        size.toInt()
    }

actual fun flushPlatform(fd: RawFileDescriptor): Result<Unit> = Result.success(Unit)

actual fun newPipe(): Result<Pipe> =
    runCatching {
        val pair = createPair(false)
        Pipe(pair.first, pair.second)
    }

actual fun socketpairImpl(): Result<Pair<FileDescriptor, FileDescriptor>> = runCatching { createPair(true) }

private fun createPair(socket: Boolean): Pair<FileDescriptor, FileDescriptor> =
    memScoped {
        val fds = allocArray<IntVar>(2)
        val typeFlags = constants.sock_stream or if (atomicCloseOnExec) constants.sock_cloexec else 0
        val result = if (socket) systemSocketpair(constants.af_unix, typeFlags, 0, fds) else createRawPipe(fds)
        if (result == -1) {
            val cause = systemError()
            throw if (socket) Error.Socketpair(cause) else Error.Pipe(cause)
        }
        val type = if (socket) HandleType.Socket else HandleType.Pipe
        val first = FileDescriptor(OwnedHandle(fds[0].toLong(), type))
        val second = FileDescriptor(OwnedHandle(fds[1].toLong(), type))
        try {
            if (!atomicCloseOnExec) {
                cloexec(first.asRawFileDescriptor()).getOrThrow()
                cloexec(second.asRawFileDescriptor()).getOrThrow()
            }
            Pair(first, second)
        } catch (error: Throwable) {
            first.close()
            second.close()
            throw error
        }
    }

actual fun pollImpl(pfd: List<Pollfd>, duration: Duration?): Result<Int> =
    runCatching {
        require(duration == null || duration >= Duration.ZERO) { "Polling duration must not be negative" }
        pfd.forEach { it.revents = 0 }
        val timeout = duration?.takeUnless { it.isInfinite() }
        if (Platform.osFamily == OsFamily.MACOSX) selectPoll(pfd, timeout) else systemPoll(pfd, timeout)
    }

private fun systemPoll(pfd: List<Pollfd>, duration: Duration?): Int =
    memScoped {
        val fds =
            pfd
                .map { item ->
                    val fd = if (item.fd < 0 && item.fd >= Int.MIN_VALUE) item.fd.toInt() else checkedFd(item.fd)
                    LibcPollfd(fd, item.events, 0)
                }.toTypedArray()
        val start = TimeSource.Monotonic.markNow()
        var remaining = duration
        var result: Int
        do {
            val millis =
                remaining?.let {
                    val whole = it.inWholeMilliseconds
                    val rounded = if (it > whole.milliseconds) whole + 1 else whole
                    rounded.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                } ?: -1
            result = libcPoll(fds, millis)
            if (result < 0) throw Error.Poll(systemError())
            remaining = duration?.minus(start.elapsedNow())
        } while (result == 0 && remaining != null && remaining > Duration.ZERO)
        pfd.forEachIndexed { index, item -> item.revents = fds[index].revents }
        result
    }

private class NativeFdSet : AutoCloseable {
    val pointer = libc_fd_set_alloc() ?: throw Error.Io(systemError())

    fun add(fd: Int) {
        libc_fd_set(fd, pointer)
    }

    fun contains(fd: Int): Boolean = libc_fd_isset(fd, pointer) != 0

    override fun close() {
        libc_fd_set_free(pointer)
    }
}

private fun selectPoll(pfd: List<Pollfd>, duration: Duration?): Int =
    memScoped {
        val descriptors =
            pfd.map {
                val fd = checkedFd(it.fd)
                if (fd >= libc_fd_setsize()) throw Error.FdValueOutsideFdSetSize(it.fd)
                fd
            }
        NativeFdSet().use { reads ->
            NativeFdSet().use { writes ->
                NativeFdSet().use { errors ->
                    pfd.forEachIndexed { index, item ->
                        val fd = descriptors[index]
                        if ((item.events.toInt() and POLLIN.toInt()) != 0) reads.add(fd)
                        if ((item.events.toInt() and POLLOUT.toInt()) != 0) writes.add(fd)
                        errors.add(fd)
                    }
                    val seconds = duration?.inWholeSeconds ?: 0L
                    val microseconds = duration?.toComponents { _, nanoseconds -> nanoseconds / 1000 } ?: 0
                    val result =
                        libc_select_timeout(
                            (descriptors.maxOrNull() ?: -1) + 1,
                            reads.pointer,
                            writes.pointer,
                            errors.pointer,
                            if (duration == null) 0 else 1,
                            seconds,
                            microseconds.toLong(),
                        )
                    if (result < 0) throw Error.Io(systemError())
                    pfd.forEachIndexed { index, item ->
                        val fd = descriptors[index]
                        var events = 0
                        if (reads.contains(fd)) events = events or POLLIN.toInt()
                        if (writes.contains(fd)) events = events or POLLOUT.toInt()
                        if (errors.contains(fd)) events = events or POLLERR.toInt()
                        item.revents = events.toShort()
                    }
                    result
                }
            }
        }
    }
