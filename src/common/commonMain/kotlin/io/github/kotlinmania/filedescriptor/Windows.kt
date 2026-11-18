// port-lint: source windows.rs
package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.libc.time.StdDuration

internal const val STD_INPUT_HANDLE: Long = 4294967286L
internal const val STD_OUTPUT_HANDLE: Long = 4294967285L
internal const val STD_ERROR_HANDLE: Long = 4294967284L

internal const val FILE_TYPE_CHAR: Int = 0x0002
internal const val FILE_TYPE_DISK: Int = 0x0001
internal const val FILE_TYPE_PIPE: Int = 0x0003
internal const val INVALID_HANDLE_VALUE: Long = -1L
internal const val INVALID_SOCKET: Long = -1L

internal fun defaultHandleTypeWindows(): HandleType = HandleType.Unknown

internal fun probeHandleTypeIfUnknown(handle: Long, handleType: HandleType): HandleType =
    when (handleType) {
        HandleType.Unknown -> probeHandleTypePlatform(handle)
        else -> handleType
    }

internal fun probeHandleTypeWindows(handle: Long): HandleType = probeHandleTypePlatform(handle)

internal fun isSocketHandle(handle: Long, handleType: HandleType): Boolean =
    when (handleType) {
        HandleType.Socket -> true
        HandleType.Unknown -> probeHandleTypePlatform(handle) == HandleType.Socket
        else -> false
    }

internal fun fromRawHandle(handle: Long): OwnedHandle = OwnedHandle.fromRaw(handle)

internal fun asRawHandle(handle: OwnedHandle): Long = handle.asRawFileDescriptor()

internal fun intoRawHandle(handle: OwnedHandle): Long = handle.intoRawFileDescriptor()

internal fun fromRawSocket(socket: Long): OwnedHandle = OwnedHandle(socket, HandleType.Socket)

internal fun asRawSocket(handle: OwnedHandle): Long = handle.asRawFileDescriptor()

internal fun intoRawSocket(handle: OwnedHandle): Long = handle.intoRawFileDescriptor()

internal fun asSocket(handle: OwnedHandle): Long = handle.asRawFileDescriptor()

internal fun asRawFileDescriptorWindows(fd: AsRawFileDescriptor): Long = fd.asRawFileDescriptor()

internal fun intoRawFileDescriptorWindows(fd: IntoRawFileDescriptor): Long = fd.intoRawFileDescriptor()

internal fun fromRawFileDescriptorWindows(fd: Long): OwnedHandle = OwnedHandle.fromRaw(fd)

internal fun asSocketDescriptorWindows(fd: AsRawSocketDescriptor): Long = fd.asSocketDescriptor()

internal fun intoSocketDescriptorWindows(fd: IntoRawSocketDescriptor): Long = fd.intoSocketDescriptor()

internal fun fromSocketDescriptorWindows(fd: Long): OwnedHandle = OwnedHandle(fd, HandleType.Socket)

internal fun dupImplWindows(f: AsRawFileDescriptor, handleType: HandleType): Result<OwnedHandle> = dupImpl(f, handleType)

internal fun asStdioImplWindows(fd: FileDescriptor): Result<DescriptorStdio> = asStdioImpl(fd)

internal fun asFileImplWindows(fd: FileDescriptor): Result<DescriptorFile> = asFileImpl(fd)

internal fun setNonBlockingImplWindows(fd: FileDescriptor, nonBlocking: Boolean): Result<Unit> =
    if (isSocketHandle(fd.asRawFileDescriptor(), fd.handle.handleType)) {
        Result.success(Unit)
    } else {
        Result.failure(FileDescriptorFailure.OnlySocketsNonBlocking)
    }

internal fun redirectStdioImplWindows(f: AsRawFileDescriptor, stdio: StdioDescriptor): Result<FileDescriptor> = redirectStdioImpl(f, stdio)

internal fun readWindows(fd: FileDescriptor, buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): ReadResult = fd.read(buf, offset, length)

internal fun writeWindows(fd: FileDescriptor, buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): WriteResult = fd.write(buf, offset, length)

internal fun flushWindows(fd: FileDescriptor): DescriptorStatus = fd.flush()

internal fun newPipeWindows(): PipeResult = Pipe.new()

internal fun initWinsock(): Result<Unit> = Result.success(Unit)

internal fun socket(af: Int, type: Int, proto: Int): Result<OwnedHandle> = Result.success(OwnedHandle(101L, HandleType.Socket))

internal fun windowsSocketpair(): Result<DescriptorPair> = socketpairImpl()

internal fun windowsPoll(pfd: List<Pollfd>, duration: StdDuration? = null): Result<Int> = pollImpl(pfd, duration)
