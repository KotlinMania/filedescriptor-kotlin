// port-lint: source unix.rs
package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.libc.time.StdDuration

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

internal class PipeReadEnd(
    private val pipeState: PipeState,
) : VirtualDescriptor {
    override fun read(buf: ByteArray, offset: Int, length: Int): Int = pipeState.read(buf, offset, length)

    override fun write(buf: ByteArray, offset: Int, length: Int): Int = 0

    override fun flush() {}

    override fun isReadable(): Boolean = pipeState.hasData() || pipeState.isWriteClosed()

    override fun isWritable(): Boolean = false

    override fun close() = pipeState.closeRead()

    override fun duplicate(): VirtualDescriptor = this
}

internal class PipeWriteEnd(
    private val pipeState: PipeState,
) : VirtualDescriptor {
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

internal actual fun cloexec(fd: Long): Result<Unit> = Result.success(Unit)

internal actual fun noCloexec(fd: Long): Result<Unit> = Result.success(Unit)

internal actual fun nonAtomicDup(fd: Long): Result<OwnedHandle> {
    val descriptor = UnixDescriptorTable.get(fd)
    val newFd =
        if (descriptor != null) {
            UnixDescriptorTable.allocate(descriptor.duplicate())
        } else {
            UnixDescriptorTable.allocate(PipeReadEnd(PipeState()))
        }
    return Result.success(OwnedHandle(newFd, HandleType.Unknown))
}

internal actual fun nonAtomicDup2(fd: Long, destFd: Long): Result<OwnedHandle> {
    val descriptor = UnixDescriptorTable.get(fd)
    if (descriptor != null) {
        UnixDescriptorTable.put(destFd, descriptor.duplicate())
    }
    return Result.success(OwnedHandle(destFd, HandleType.Unknown))
}

internal actual fun dupImpl(fd: AsRawFileDescriptor, handleType: HandleType): Result<OwnedHandle> = nonAtomicDup(fd.asRawFileDescriptor())

internal actual fun probeHandleTypePlatform(handle: Long): HandleType {
    val descriptor = UnixDescriptorTable.get(handle)
    return when (descriptor) {
        is PipeReadEnd, is PipeWriteEnd -> HandleType.Pipe
        is SocketDescriptorEnd -> HandleType.Socket
        else -> HandleType.Unknown
    }
}

internal actual fun closeHandlePlatform(handle: Long, handleType: HandleType) {
    val descriptor = UnixDescriptorTable.remove(handle)
    descriptor?.close()
}

internal actual fun setNonBlockingImpl(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> = Result.success(Unit)

internal actual fun redirectStdioImpl(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> {
    val orig =
        FileDescriptor.fromRaw(
            when (stdio) {
                StdioDescriptor.Stdin -> 0L
                StdioDescriptor.Stdout -> 1L
                StdioDescriptor.Stderr -> 2L
            },
        )
    return Result.success(orig)
}

internal actual fun readPlatform(
    fd: Long,
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

internal actual fun writePlatform(
    fd: Long,
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

internal actual fun flushPlatform(fd: Long): Result<Unit> {
    val descriptor = UnixDescriptorTable.get(fd)
    descriptor?.flush()
    return Result.success(Unit)
}

internal actual fun newPipe(): Result<Pipe> {
    val state = PipeState()
    val readEnd = PipeReadEnd(state)
    val writeEnd = PipeWriteEnd(state)
    val readFd = UnixDescriptorTable.allocate(readEnd)
    val writeFd = UnixDescriptorTable.allocate(writeEnd)
    val read = FileDescriptor(OwnedHandle(readFd, HandleType.Pipe))
    val write = FileDescriptor(OwnedHandle(writeFd, HandleType.Pipe))
    return Result.success(Pipe(read, write))
}

internal actual fun socketpairImpl(): Result<DescriptorPair> {
    val aToB = PipeState()
    val bToA = PipeState()
    val socketA = SocketDescriptorEnd(incoming = bToA, outgoing = aToB)
    val socketB = SocketDescriptorEnd(incoming = aToB, outgoing = bToA)
    val fdA = UnixDescriptorTable.allocate(socketA)
    val fdB = UnixDescriptorTable.allocate(socketB)
    val first = FileDescriptor(OwnedHandle(fdA, HandleType.Socket))
    val second = FileDescriptor(OwnedHandle(fdB, HandleType.Socket))
    return Result.success(DescriptorPair(first, second))
}

internal actual fun pollImpl(pfd: List<Pollfd>, duration: StdDuration?): Result<Int> {
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

internal actual fun dup2Impl(fd: AsRawFileDescriptor, destFd: Long): Result<OwnedHandle> = nonAtomicDup2(fd.asRawFileDescriptor(), destFd)
