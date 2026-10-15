// port-lint: source unix.rs
package io.github.kotlinmania.filedescriptor

import kotlin.time.Duration

/**
 * FdSet representation for Unix select operations.
 */
class FdSet {
    private val fds = mutableSetOf<Long>()

    fun checkFd(fd: Long): Result<Unit> {
        if (fd < 0) {
            return Result.failure(Error.IllegalFdValue(fd))
        }
        if (fd >= 1024) {
            return Result.failure(Error.FdValueOutsideFdSetSize(fd))
        }
        return Result.success(Unit)
    }

    fun add(fd: Long) {
        fds.add(fd)
    }

    fun contains(fd: Long): Boolean = fds.contains(fd)

    fun materialize(): Set<Long> = fds.toSet()

    fun setPtr(): Any? = null

    fun isSet(fd: Long): Boolean = fds.contains(fd)
}

expect fun cloexec(fd: Long): Result<Unit>

expect fun noCloexec(fd: Long): Result<Unit>

expect fun nonAtomicDup(fd: Long): Result<OwnedHandle>

expect fun nonAtomicDup2(fd: Long, destFd: Long): Result<OwnedHandle>

expect fun dupImpl(fd: AsRawFileDescriptor, handleType: HandleType): Result<OwnedHandle>

expect fun dup2Impl(fd: AsRawFileDescriptor, destFd: Long): Result<OwnedHandle>

expect fun probeHandleTypePlatform(handle: Long): HandleType

expect fun closeHandlePlatform(handle: Long, handleType: HandleType)

fun dup2(f: AsRawFileDescriptor, destFd: Long): Result<FileDescriptor> = dup2Impl(f, destFd).map { FileDescriptor(it) }

fun dup2Platform(fd: AsRawFileDescriptor, destFd: Long): Result<OwnedHandle> = dup2Impl(fd, destFd)

expect fun asStdioImpl(fd: FileDescriptor): Result<Any?>

expect fun asFileImpl(fd: FileDescriptor): Result<Any?>

expect fun setNonBlockingImpl(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit>

expect fun redirectStdioImpl(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor>

fun asRawFileDescriptor(fd: AsRawFileDescriptor): Long = fd.asRawFileDescriptor()

fun intoRawFileDescriptor(fd: IntoRawFileDescriptor): Long = fd.intoRawFileDescriptor()

fun fromRawFileDescriptor(fd: Long): OwnedHandle = OwnedHandle.fromRaw(fd)

fun asSocketDescriptor(fd: AsRawSocketDescriptor): Long = fd.asSocketDescriptor()

fun intoSocketDescriptor(fd: IntoRawSocketDescriptor): Long = fd.intoSocketDescriptor()

fun fromSocketDescriptor(fd: Long): OwnedHandle = OwnedHandle(fd, HandleType.Socket)

fun asFd(fd: AsRawFileDescriptor): Long = fd.asRawFileDescriptor()

fun asRawFd(fd: AsRawFileDescriptor): Long = fd.asRawFileDescriptor()

fun intoRawFd(fd: IntoRawFileDescriptor): Long = fd.intoRawFileDescriptor()

fun fromRawFd(fd: Long): OwnedHandle = OwnedHandle.fromRaw(fd)

expect fun readPlatform(
    fd: Long,
    handleType: HandleType,
    buf: ByteArray,
    offset: Int,
    length: Int,
): Result<Int>

expect fun writePlatform(
    fd: Long,
    handleType: HandleType,
    buf: ByteArray,
    offset: Int,
    length: Int,
): Result<Int>

expect fun flushPlatform(fd: Long): Result<Unit>

fun asStdioPlatform(fd: FileDescriptor): Result<Any?> = asStdioImpl(fd)

fun asFilePlatform(fd: FileDescriptor): Result<Any?> = asFileImpl(fd)

fun setNonBlockingPlatform(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> = setNonBlockingImpl(fd, nonBlocking)

fun redirectStdioPlatform(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> =
    redirectStdioImpl(f, stdio)

fun createPipePlatform(): Result<Pipe> = newPipe()

expect fun newPipe(): Result<Pipe>

fun socketpairPlatform(): Result<Pair<FileDescriptor, FileDescriptor>> = socketpairImpl()

expect fun socketpairImpl(): Result<Pair<FileDescriptor, FileDescriptor>>

fun pollPlatform(pfd: List<Pollfd>, duration: Duration?): Result<Int> = pollImpl(pfd, duration)

expect fun pollImpl(pfd: List<Pollfd>, duration: Duration?): Result<Int>

fun readToStringPlatform(fd: FileDescriptor): Result<String> {
    val bytes = mutableListOf<Byte>()
    val buf = ByteArray(1024)
    while (true) {
        val n = fd.read(buf).getOrThrow()
        if (n <= 0) break
        for (i in 0 until n) {
            bytes.add(buf[i])
        }
    }
    return Result.success(bytes.toByteArray().decodeToString())
}
