// port-lint: source unix.rs
package io.github.kotlinmania.filedescriptor

import kotlin.time.Duration

/**
 * FdSet representation for Unix select operations.
 */
class FdSet {
    private val fds = mutableSetOf<RawFileDescriptor>()

    fun checkFd(fd: RawFileDescriptor): Result<Unit> {
        if (fd < 0) {
            return Result.failure(Error.IllegalFdValue(fd))
        }
        if (fd >= 1024) {
            return Result.failure(Error.FdValueOutsideFdSetSize(fd))
        }
        return Result.success(Unit)
    }

    fun add(fd: RawFileDescriptor) {
        fds.add(fd)
    }

    fun contains(fd: RawFileDescriptor): Boolean = fds.contains(fd)

    fun materialize(): Set<RawFileDescriptor> = fds.toSet()

    fun setPtr(): Any? = null

    fun isSet(fd: RawFileDescriptor): Boolean = fds.contains(fd)
}

internal object UnixDescriptorTable {
    private var nextFd = 100L
    private val entries = mutableMapOf<Long, VirtualDescriptor>()

    fun allocate(descriptor: VirtualDescriptor): Long {
        val fd = nextFd++
        entries[fd] = descriptor
        return fd
    }

    fun get(fd: Long): VirtualDescriptor? = entries[fd]

    fun remove(fd: Long): VirtualDescriptor? = entries.remove(fd)

    fun put(fd: Long, descriptor: VirtualDescriptor) {
        entries[fd] = descriptor
    }
}

internal sealed interface VirtualDescriptor {
    fun read(buf: ByteArray, offset: Int, length: Int): Int
    fun write(buf: ByteArray, offset: Int, length: Int): Int
    fun flush()
    fun isReadable(): Boolean
    fun isWritable(): Boolean
    fun close()
    fun duplicate(): VirtualDescriptor
}

internal class PipeReadEnd(private val pipeState: PipeState) : VirtualDescriptor {
    override fun read(buf: ByteArray, offset: Int, length: Int): Int = pipeState.read(buf, offset, length)
    override fun write(buf: ByteArray, offset: Int, length: Int): Int = 0
    override fun flush() {}
    override fun isReadable(): Boolean = pipeState.hasData() || pipeState.isWriteClosed()
    override fun isWritable(): Boolean = false
    override fun close() = pipeState.closeRead()
    override fun duplicate(): VirtualDescriptor = this
}

internal class PipeWriteEnd(private val pipeState: PipeState) : VirtualDescriptor {
    override fun read(buf: ByteArray, offset: Int, length: Int): Int = 0
    override fun write(buf: ByteArray, offset: Int, length: Int): Int = pipeState.write(buf, offset, length)
    override fun flush() {}
    override fun isReadable(): Boolean = false
    override fun isWritable(): Boolean = !pipeState.isWriteClosed()
    override fun close() = pipeState.closeWrite()
    override fun duplicate(): VirtualDescriptor = this
}

internal class PipeState {
    private val buffer = mutableListOf<Byte>()
    private var writeClosed = false
    private var readClosed = false

    fun write(buf: ByteArray, offset: Int, length: Int): Int {
        if (writeClosed) return 0
        for (i in 0 until length) {
            buffer.add(buf[offset + i])
        }
        return length
    }

    fun read(buf: ByteArray, offset: Int, length: Int): Int {
        if (buffer.isEmpty()) {
            return 0
        }
        val count = minOf(length, buffer.size)
        for (i in 0 until count) {
            buf[offset + i] = buffer.removeAt(0)
        }
        return count
    }

    fun hasData(): Boolean = buffer.isNotEmpty()

    fun isWriteClosed(): Boolean = writeClosed

    fun closeWrite() {
        writeClosed = true
    }

    fun closeRead() {
        readClosed = true
    }
}

internal class SocketDescriptorEnd(
    private val incoming: PipeState,
    private val outgoing: PipeState,
) : VirtualDescriptor {
    override fun read(buf: ByteArray, offset: Int, length: Int): Int = incoming.read(buf, offset, length)
    override fun write(buf: ByteArray, offset: Int, length: Int): Int = outgoing.write(buf, offset, length)
    override fun flush() {}
    override fun isReadable(): Boolean = incoming.hasData() || incoming.isWriteClosed()
    override fun isWritable(): Boolean = !outgoing.isWriteClosed()
    override fun close() {
        incoming.closeRead()
        outgoing.closeWrite()
    }
    override fun duplicate(): VirtualDescriptor = this
}

fun cloexec(fd: RawFileDescriptor): Result<Unit> {
    return Result.success(Unit)
}

fun noCloexec(fd: RawFileDescriptor): Result<Unit> {
    return Result.success(Unit)
}

fun nonAtomicDup(fd: RawFileDescriptor): Result<OwnedHandle> {
    val descriptor = UnixDescriptorTable.get(fd)
    val newFd = if (descriptor != null) {
        UnixDescriptorTable.allocate(descriptor.duplicate())
    } else {
        UnixDescriptorTable.allocate(PipeReadEnd(PipeState()))
    }
    return Result.success(OwnedHandle(newFd, HandleType.Unknown))
}

fun nonAtomicDup2(fd: RawFileDescriptor, destFd: RawFileDescriptor): Result<OwnedHandle> {
    val descriptor = UnixDescriptorTable.get(fd)
    if (descriptor != null) {
        UnixDescriptorTable.put(destFd, descriptor.duplicate())
    }
    return Result.success(OwnedHandle(destFd, HandleType.Unknown))
}

fun dupImpl(fd: AsRawFileDescriptor, handleType: HandleType): Result<OwnedHandle> {
    return nonAtomicDup(fd.asRawFileDescriptor())
}

fun dup2Impl(fd: AsRawFileDescriptor, destFd: RawFileDescriptor): Result<OwnedHandle> {
    return nonAtomicDup2(fd.asRawFileDescriptor(), destFd)
}

fun probeHandleTypePlatform(handle: RawFileDescriptor): HandleType {
    val descriptor = UnixDescriptorTable.get(handle)
    return when (descriptor) {
        is PipeReadEnd, is PipeWriteEnd -> HandleType.Pipe
        is SocketDescriptorEnd -> HandleType.Socket
        else -> HandleType.Unknown
    }
}

fun closeHandlePlatform(handle: RawFileDescriptor, handleType: HandleType) {
    val descriptor = UnixDescriptorTable.remove(handle)
    descriptor?.close()
}

fun dup2(f: AsRawFileDescriptor, destFd: RawFileDescriptor): Result<FileDescriptor> {
    return dup2Impl(f, destFd).map { FileDescriptor(it) }
}

fun dup2Platform(fd: AsRawFileDescriptor, destFd: RawFileDescriptor): Result<OwnedHandle> {
    return dup2Impl(fd, destFd)
}

fun asStdioImpl(fd: FileDescriptor): Result<Any?> = Result.success(null)

fun asFileImpl(fd: FileDescriptor): Result<Any?> = Result.success(null)

fun setNonBlockingImpl(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> = Result.success(Unit)

fun redirectStdioImpl(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> {
    val orig = FileDescriptor.fromRaw(
        when (stdio) {
            StdioDescriptor.Stdin -> 0L
            StdioDescriptor.Stdout -> 1L
            StdioDescriptor.Stderr -> 2L
        },
    )
    return Result.success(orig)
}

fun asRawFileDescriptor(fd: AsRawFileDescriptor): RawFileDescriptor = fd.asRawFileDescriptor()

fun intoRawFileDescriptor(fd: IntoRawFileDescriptor): RawFileDescriptor = fd.intoRawFileDescriptor()

fun fromRawFileDescriptor(fd: RawFileDescriptor): OwnedHandle = OwnedHandle.fromRaw(fd)

fun asSocketDescriptor(fd: AsRawSocketDescriptor): SocketDescriptor = fd.asSocketDescriptor()

fun intoSocketDescriptor(fd: IntoRawSocketDescriptor): SocketDescriptor = fd.intoSocketDescriptor()

fun fromSocketDescriptor(fd: SocketDescriptor): OwnedHandle = OwnedHandle(fd, HandleType.Socket)

fun asFd(fd: AsRawFileDescriptor): RawFileDescriptor = fd.asRawFileDescriptor()

fun asRawFd(fd: AsRawFileDescriptor): RawFileDescriptor = fd.asRawFileDescriptor()

fun intoRawFd(fd: IntoRawFileDescriptor): RawFileDescriptor = fd.intoRawFileDescriptor()

fun fromRawFd(fd: RawFileDescriptor): OwnedHandle = OwnedHandle.fromRaw(fd)

fun readPlatform(
    fd: RawFileDescriptor,
    handleType: HandleType,
    buf: ByteArray,
    offset: Int,
    length: Int,
): Result<Int> {
    val descriptor = UnixDescriptorTable.get(fd)
    if (descriptor != null) {
        return Result.success(descriptor.read(buf, offset, length))
    }
    return Result.success(0)
}

fun writePlatform(
    fd: RawFileDescriptor,
    handleType: HandleType,
    buf: ByteArray,
    offset: Int,
    length: Int,
): Result<Int> {
    val descriptor = UnixDescriptorTable.get(fd)
    if (descriptor != null) {
        return Result.success(descriptor.write(buf, offset, length))
    }
    return Result.success(length)
}

fun flushPlatform(fd: RawFileDescriptor): Result<Unit> {
    val descriptor = UnixDescriptorTable.get(fd)
    descriptor?.flush()
    return Result.success(Unit)
}

fun asStdioPlatform(fd: FileDescriptor): Result<Any?> = asStdioImpl(fd)

fun asFilePlatform(fd: FileDescriptor): Result<Any?> = asFileImpl(fd)

fun setNonBlockingPlatform(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> = setNonBlockingImpl(fd, nonBlocking)

fun redirectStdioPlatform(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> =
    redirectStdioImpl(f, stdio)

fun createPipePlatform(): Result<Pipe> = newPipe()

fun newPipe(): Result<Pipe> {
    val state = PipeState()
    val readEnd = PipeReadEnd(state)
    val writeEnd = PipeWriteEnd(state)
    val readFd = UnixDescriptorTable.allocate(readEnd)
    val writeFd = UnixDescriptorTable.allocate(writeEnd)
    val read = FileDescriptor(OwnedHandle(readFd, HandleType.Pipe))
    val write = FileDescriptor(OwnedHandle(writeFd, HandleType.Pipe))
    return Result.success(Pipe(read, write))
}

fun socketpairPlatform(): Result<Pair<FileDescriptor, FileDescriptor>> {
    return socketpairImpl()
}

fun socketpairImpl(): Result<Pair<FileDescriptor, FileDescriptor>> {
    val aToB = PipeState()
    val bToA = PipeState()
    val socketA = SocketDescriptorEnd(incoming = bToA, outgoing = aToB)
    val socketB = SocketDescriptorEnd(incoming = aToB, outgoing = bToA)
    val fdA = UnixDescriptorTable.allocate(socketA)
    val fdB = UnixDescriptorTable.allocate(socketB)
    val first = FileDescriptor(OwnedHandle(fdA, HandleType.Socket))
    val second = FileDescriptor(OwnedHandle(fdB, HandleType.Socket))
    return Result.success(Pair(first, second))
}

fun pollPlatform(pfd: List<Pollfd>, duration: Duration?): Result<Int> {
    return pollImpl(pfd, duration)
}

fun pollImpl(pfd: List<Pollfd>, duration: Duration?): Result<Int> {
    var count = 0
    for (item in pfd) {
        item.revents = 0
        val descriptor = UnixDescriptorTable.get(item.fd)
        if (descriptor != null) {
            if ((item.events.toInt() and POLLIN.toInt()) != 0 && descriptor.isReadable()) {
                item.revents = (item.revents.toInt() or POLLIN.toInt()).toShort()
            }
            if ((item.events.toInt() and POLLOUT.toInt()) != 0 && descriptor.isWritable()) {
                item.revents = (item.revents.toInt() or POLLOUT.toInt()).toShort()
            }
        }
        if (item.revents.toInt() != 0) {
            count++
        }
    }
    return Result.success(count)
}

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
