// port-lint: source windows.rs
package io.github.kotlinmania.filedescriptor

import kotlin.time.Duration

const val STD_INPUT_HANDLE: Long = 4294967286L
const val STD_OUTPUT_HANDLE: Long = 4294967285L
const val STD_ERROR_HANDLE: Long = 4294967284L

const val FILE_TYPE_CHAR: Int = 0x0002
const val FILE_TYPE_DISK: Int = 0x0001
const val FILE_TYPE_PIPE: Int = 0x0003
const val INVALID_HANDLE_VALUE: Long = -1L
const val INVALID_SOCKET: Long = -1L

fun defaultHandleTypeWindows(): HandleType = HandleType.Unknown

fun probeHandleTypeIfUnknown(handle: RawFileDescriptor, handleType: HandleType): HandleType {
    return when (handleType) {
        HandleType.Unknown -> probeHandleTypePlatform(handle)
        else -> handleType
    }
}

fun probeHandleTypeWindows(handle: RawFileDescriptor): HandleType = probeHandleTypePlatform(handle)

fun isSocketHandle(handle: RawFileDescriptor, handleType: HandleType): Boolean {
    return when (handleType) {
        HandleType.Socket -> true
        HandleType.Unknown -> probeHandleTypePlatform(handle) == HandleType.Socket
        else -> false
    }
}

fun fromRawHandle(handle: RawFileDescriptor): OwnedHandle {
    return OwnedHandle.fromRaw(handle)
}

fun asRawHandle(handle: OwnedHandle): RawFileDescriptor {
    return handle.asRawFileDescriptor()
}

fun intoRawHandle(handle: OwnedHandle): RawFileDescriptor {
    return handle.intoRawFileDescriptor()
}

fun fromRawSocket(socket: SocketDescriptor): OwnedHandle {
    return OwnedHandle(socket, HandleType.Socket)
}

fun asRawSocket(handle: OwnedHandle): SocketDescriptor {
    return handle.asRawFileDescriptor()
}

fun intoRawSocket(handle: OwnedHandle): SocketDescriptor {
    return handle.intoRawFileDescriptor()
}

fun asSocket(handle: OwnedHandle): SocketDescriptor {
    return handle.asRawFileDescriptor()
}

fun asRawFileDescriptorWindows(fd: AsRawFileDescriptor): RawFileDescriptor = fd.asRawFileDescriptor()

fun intoRawFileDescriptorWindows(fd: IntoRawFileDescriptor): RawFileDescriptor = fd.intoRawFileDescriptor()

fun fromRawFileDescriptorWindows(fd: RawFileDescriptor): OwnedHandle = OwnedHandle.fromRaw(fd)

fun asSocketDescriptorWindows(fd: AsRawSocketDescriptor): SocketDescriptor = fd.asSocketDescriptor()

fun intoSocketDescriptorWindows(fd: IntoRawSocketDescriptor): SocketDescriptor = fd.intoSocketDescriptor()

fun fromSocketDescriptorWindows(fd: SocketDescriptor): OwnedHandle = OwnedHandle(fd, HandleType.Socket)

fun dupImplWindows(f: AsRawFileDescriptor, handleType: HandleType): Result<OwnedHandle> {
    return dupImpl(f, handleType)
}

fun asStdioImplWindows(fd: FileDescriptor): Result<Any?> = Result.success(null)

fun asFileImplWindows(fd: FileDescriptor): Result<Any?> = Result.success(null)

fun setNonBlockingImplWindows(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> {
    return if (isSocketHandle(fd.asRawFileDescriptor(), fd.handle.handleType)) {
        Result.success(Unit)
    } else {
        Result.failure(Error.OnlySocketsNonBlocking)
    }
}

fun redirectStdioImplWindows(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> {
    return redirectStdioImpl(f, stdio)
}

fun readWindows(fd: FileDescriptor, buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Result<Int> {
    return fd.read(buf, offset, length)
}

fun writeWindows(fd: FileDescriptor, buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Result<Int> {
    return fd.write(buf, offset, length)
}

fun flushWindows(fd: FileDescriptor): Result<Unit> {
    return fd.flush()
}

fun newPipeWindows(): Result<Pipe> {
    return Pipe.new()
}

fun initWinsock(): Result<Unit> {
    return Result.success(Unit)
}

fun socket(af: Int, type: Int, proto: Int): Result<OwnedHandle> {
    return Result.success(OwnedHandle(101L, HandleType.Socket))
}

fun windowsSocketpair(): Result<Pair<FileDescriptor, FileDescriptor>> {
    return socketpairImpl()
}

fun windowsPoll(pfd: List<Pollfd>, duration: Duration? = null): Result<Int> {
    return pollImpl(pfd, duration)
}
