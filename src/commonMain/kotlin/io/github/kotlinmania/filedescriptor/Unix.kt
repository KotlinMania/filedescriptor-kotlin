// port-lint: source unix.rs
package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.libc.time.StdDuration

/**
 * FdSet representation for Unix select operations.
 */
internal class FdSet {
    private val fds = mutableSetOf<Long>()

    fun checkFd(fd: Long): Result<Unit> {
        if (fd < 0) {
            return Result.failure(FileDescriptorFailure.IllegalFdValue(fd))
        }
        if (fd >= 1024) {
            return Result.failure(FileDescriptorFailure.FdValueOutsideFdSetSize(fd))
        }
        return Result.success(Unit)
    }

    fun add(fd: Long) {
        fds.add(fd)
    }

    fun contains(fd: Long): Boolean = fds.contains(fd)

    fun materialize(): Set<Long> = fds.toSet()

    fun isSet(fd: Long): Boolean = fds.contains(fd)
}

internal expect fun cloexec(fd: Long): Result<Unit>

internal expect fun noCloexec(fd: Long): Result<Unit>

internal expect fun nonAtomicDup(fd: Long): Result<OwnedHandle>

internal expect fun nonAtomicDup2(fd: Long, destFd: Long): Result<OwnedHandle>

internal expect fun dupImpl(fd: AsRawFileDescriptor, handleType: HandleType): Result<OwnedHandle>

internal expect fun dup2Impl(fd: AsRawFileDescriptor, destFd: Long): Result<OwnedHandle>

internal expect fun probeHandleTypePlatform(handle: Long): HandleType

internal expect fun closeHandlePlatform(handle: Long, handleType: HandleType)

internal fun dup2(f: AsRawFileDescriptor, destFd: Long): Result<FileDescriptor> = dup2Impl(f, destFd).map { FileDescriptor(it) }

internal fun dup2Platform(fd: AsRawFileDescriptor, destFd: Long): Result<OwnedHandle> = dup2Impl(fd, destFd)

internal fun asStdioImpl(fd: FileDescriptor): Result<DescriptorStdio> =
    dupImpl(fd, fd.handle.handleType).map { DescriptorStdio(FileDescriptor(it)) }

internal fun asFileImpl(fd: FileDescriptor): Result<DescriptorFile> =
    dupImpl(fd, fd.handle.handleType).map { DescriptorFile(FileDescriptor(it)) }

internal expect fun setNonBlockingImpl(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit>

internal expect fun redirectStdioImpl(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor>

internal fun asRawFileDescriptor(fd: AsRawFileDescriptor): Long = fd.asRawFileDescriptor()

internal fun intoRawFileDescriptor(fd: IntoRawFileDescriptor): Long = fd.intoRawFileDescriptor()

internal fun fromRawFileDescriptor(fd: Long): OwnedHandle = OwnedHandle.fromRaw(fd)

internal fun asSocketDescriptor(fd: AsRawSocketDescriptor): Long = fd.asSocketDescriptor()

internal fun intoSocketDescriptor(fd: IntoRawSocketDescriptor): Long = fd.intoSocketDescriptor()

internal fun fromSocketDescriptor(fd: Long): OwnedHandle = OwnedHandle(fd, HandleType.Socket)

internal fun asFd(fd: AsRawFileDescriptor): Long = fd.asRawFileDescriptor()

internal fun asRawFd(fd: AsRawFileDescriptor): Long = fd.asRawFileDescriptor()

internal fun intoRawFd(fd: IntoRawFileDescriptor): Long = fd.intoRawFileDescriptor()

internal fun fromRawFd(fd: Long): OwnedHandle = OwnedHandle.fromRaw(fd)

internal expect fun readPlatform(
    fd: Long,
    handleType: HandleType,
    buf: ByteArray,
    offset: Int,
    length: Int,
): Result<Int>

internal expect fun writePlatform(
    fd: Long,
    handleType: HandleType,
    buf: ByteArray,
    offset: Int,
    length: Int,
): Result<Int>

internal expect fun flushPlatform(fd: Long): Result<Unit>

internal fun asStdioPlatform(fd: FileDescriptor): Result<DescriptorStdio> = asStdioImpl(fd)

internal fun asFilePlatform(fd: FileDescriptor): Result<DescriptorFile> = asFileImpl(fd)

internal fun setNonBlockingPlatform(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> = setNonBlockingImpl(fd, nonBlocking)

internal fun redirectStdioPlatform(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> =
    redirectStdioImpl(f, stdio)

internal fun createPipePlatform(): Result<Pipe> = newPipe()

internal expect fun newPipe(): Result<Pipe>

internal fun socketpairPlatform(): Result<DescriptorPair> = socketpairImpl()

internal expect fun socketpairImpl(): Result<DescriptorPair>

internal fun pollPlatform(pfd: List<Pollfd>, duration: StdDuration?): Result<Int> = pollImpl(pfd, duration)

internal expect fun pollImpl(pfd: List<Pollfd>, duration: StdDuration?): Result<Int>

internal fun readToStringPlatform(fd: FileDescriptor): Result<String> =
    runCatching {
        val bytes = mutableListOf<Byte>()
        val buf = ByteArray(1024)
        while (true) {
            val n = fd.read(buf).getOrThrow()
            if (n <= 0) break
            for (i in 0 until n) {
                bytes.add(buf[i])
            }
        }
        bytes.toByteArray().decodeToString()
    }
