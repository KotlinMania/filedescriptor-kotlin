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

fun probeHandleTypeIfUnknown(handle: RawFileDescriptor, handleType: HandleType): HandleType =
    when (handleType) {
        HandleType.Unknown -> probeHandleTypePlatform(handle)
        else -> handleType
    }

fun probeHandleTypeWindows(handle: RawFileDescriptor): HandleType = probeHandleTypePlatform(handle)

fun isSocketHandle(handle: RawFileDescriptor, handleType: HandleType): Boolean =
    when (handleType) {
        HandleType.Socket -> true
        HandleType.Unknown -> probeHandleTypePlatform(handle) == HandleType.Socket
        else -> false
    }

fun fromRawHandle(handle: RawFileDescriptor): OwnedHandle = OwnedHandle.fromRaw(handle)

fun asRawHandle(handle: OwnedHandle): RawFileDescriptor = handle.asRawFileDescriptor()

fun intoRawHandle(handle: OwnedHandle): RawFileDescriptor = handle.intoRawFileDescriptor()

fun fromRawSocket(socket: SocketDescriptor): OwnedHandle = OwnedHandle(socket, HandleType.Socket)

fun asRawSocket(handle: OwnedHandle): SocketDescriptor = handle.asRawFileDescriptor()

fun intoRawSocket(handle: OwnedHandle): SocketDescriptor = handle.intoRawFileDescriptor()

fun asSocket(handle: OwnedHandle): SocketDescriptor = handle.asRawFileDescriptor()

fun asRawFileDescriptorWindows(fd: AsRawFileDescriptor): RawFileDescriptor = fd.asRawFileDescriptor()

fun intoRawFileDescriptorWindows(fd: IntoRawFileDescriptor): RawFileDescriptor = fd.intoRawFileDescriptor()

fun fromRawFileDescriptorWindows(fd: RawFileDescriptor): OwnedHandle = OwnedHandle.fromRaw(fd)

fun asSocketDescriptorWindows(fd: AsRawSocketDescriptor): SocketDescriptor = fd.asSocketDescriptor()

fun intoSocketDescriptorWindows(fd: IntoRawSocketDescriptor): SocketDescriptor = fd.intoSocketDescriptor()

fun fromSocketDescriptorWindows(fd: SocketDescriptor): OwnedHandle = OwnedHandle(fd, HandleType.Socket)

fun dupImplWindows(f: AsRawFileDescriptor, handleType: HandleType): Result<OwnedHandle> = dupImpl(f, handleType)

fun asStdioImplWindows(fd: FileDescriptor): Result<Any?> = Result.success(null)

fun asFileImplWindows(fd: FileDescriptor): Result<Any?> = Result.success(null)

fun setNonBlockingImplWindows(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> =
    if (isSocketHandle(fd.asRawFileDescriptor(), fd.handle.handleType)) {
        Result.success(Unit)
    } else {
        Result.failure(Error.OnlySocketsNonBlocking)
    }

fun redirectStdioImplWindows(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> = redirectStdioImpl(f, stdio)

fun readWindows(fd: FileDescriptor, buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Result<Int> = fd.read(buf, offset, length)

fun writeWindows(fd: FileDescriptor, buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Result<Int> = fd.write(buf, offset, length)

fun flushWindows(fd: FileDescriptor): Result<Unit> = fd.flush()

fun newPipeWindows(): Result<Pipe> = Pipe.new()

fun initWinsock(): Result<Unit> = Result.success(Unit)

fun socket(af: Int, type: Int, proto: Int): Result<OwnedHandle> = Result.success(OwnedHandle(101L, HandleType.Socket))

fun windowsSocketpair(): Result<Pair<FileDescriptor, FileDescriptor>> = socketpairImpl()

fun windowsPoll(pfd: List<Pollfd>, duration: Duration? = null): Result<Int> = pollImpl(pfd, duration)
