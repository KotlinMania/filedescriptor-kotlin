@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

// port-lint: source lib.rs
package io.github.kotlinmania.filedescriptor

import kotlin.native.HiddenFromObjC
import kotlin.time.Duration

/**
 * The purpose of this package is to make it convenient for portable
 * applications that need to work with platform-level raw file descriptors
 * and handles.
 *
 * Rather than conditionally using platform descriptors, the [FileDescriptor]
 * type can be used to manage ownership, duplicate, read and write.
 *
 * ## FileDescriptor
 *
 * Demonstrates avoiding conditional code when dealing with platform descriptors:
 *
 * ```
 * fun getStdout(): Result<FileDescriptor> {
 *     val stdout = FileDescriptor.new(1L)
 *     return FileDescriptor.dup(stdout)
 * }
 *
 * fun printSomething(): Result<Unit> {
 *     return getStdout().mapCatching { it.write("hello".encodeToByteArray()) }
 * }
 * ```
 *
 * ## Pipe
 * The [Pipe] type makes it convenient to create a pipe and manage
 * the lifecycle of both the read and write ends of that pipe.
 *
 * ```
 * val pipe = Pipe.new().getOrThrow()
 * pipe.write.write("hello".encodeToByteArray())
 * pipe.write.close()
 *
 * val s = pipe.read.readToString().getOrThrow()
 * check(s == "hello")
 * ```
 *
 * ## Socketpair
 * The [socketpair] function returns a pair of connected stream
 * sockets and functions across platforms.
 *
 * ```
 * val (a, b) = socketpair().getOrThrow()
 * a.write("hello".encodeToByteArray())
 * a.close()
 *
 * val s = b.readToString().getOrThrow()
 * check(s == "hello")
 * ```
 *
 * ## Polling
 * Polling offers readiness testing of a set of file descriptors:
 *
 * ```
 * val (a, b) = socketpair().getOrThrow()
 * val pollArray = mutableListOf(Pollfd(
 *     fd = a.asSocketDescriptor(),
 *     events = POLLIN,
 *     revents = 0,
 * ))
 * check(poll(pollArray, Duration.parse("20ms")).getOrThrow() == 0)
 *
 * b.write("hello".encodeToByteArray())
 * check(poll(pollArray, Duration.parse("20ms")).getOrThrow() == 1)
 * ```
 */

/**
 * Platform-independent type alias for the underlying platform file descriptor type.
 */
typealias RawFileDescriptor = Long

/**
 * Platform-independent type alias for the underlying platform socket descriptor type.
 */
typealias SocketDescriptor = Long

/**
 * Internal classifier carried by [OwnedHandle] to record handle category.
 */
enum class HandleType {
    Char,
    Disk,
    Pipe,
    Socket,
    Unknown,
}

fun defaultHandleType(): HandleType = HandleType.Unknown

fun probeHandleType(handle: RawFileDescriptor): HandleType = probeHandleTypePlatform(handle)

class Pollfd(
    var fd: SocketDescriptor,
    var events: Short,
    var revents: Short = 0,
)

const val POLLIN: Short = 0x0001
const val POLLOUT: Short = 0x0004
const val POLLERR: Short = 0x0008
const val POLLHUP: Short = 0x0010
const val POLLPRI: Short = 0x0002
const val POLLNVAL: Short = 0x0020

/**
 * Errors raised by [FileDescriptor], [OwnedHandle], [Pipe], [poll], and
 * [socketpair].
 */
@HiddenFromObjC
sealed class Error(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    @HiddenFromObjC
    class Pipe(cause: Throwable) : Error("failed to create a pipe", cause)

    @HiddenFromObjC
    class Socketpair(cause: Throwable) : Error("failed to create a socketpair", cause)

    @HiddenFromObjC
    class Socket(cause: Throwable) : Error("failed to create a socket", cause)

    @HiddenFromObjC
    class Bind(cause: Throwable) : Error("failed to bind a socket", cause)

    @HiddenFromObjC
    class Getsockname(cause: Throwable) : Error("failed to fetch socket name", cause)

    @HiddenFromObjC
    class Listen(cause: Throwable) : Error("failed to set socket to listen mode", cause)

    @HiddenFromObjC
    class Connect(cause: Throwable) : Error("failed to connect socket", cause)

    @HiddenFromObjC
    class Accept(cause: Throwable) : Error("failed to accept socket", cause)

    @HiddenFromObjC
    class Fcntl(cause: Throwable) : Error("fcntl read failed", cause)

    @HiddenFromObjC
    class Cloexec(cause: Throwable) : Error("failed to set cloexec", cause)

    @HiddenFromObjC
    class FionBio(cause: Throwable) : Error("failed to change non-blocking mode", cause)

    @HiddenFromObjC
    class Poll(cause: Throwable) : Error("poll failed", cause)

    @HiddenFromObjC
    class Dup(val fd: Long, cause: Throwable) : Error("dup of fd $fd failed", cause)

    @HiddenFromObjC
    class Dup2(val srcFd: Long, val destFd: Long, cause: Throwable) :
        Error("dup of fd $srcFd to fd $destFd failed", cause)

    @HiddenFromObjC
    class IllegalFdValue(val fd: Long) : Error("Illegal fd value $fd")

    @HiddenFromObjC
    class FdValueOutsideFdSetSize(val fd: Long) :
        Error("fd value $fd too large to use with select(2)")

    @HiddenFromObjC
    data object OnlySocketsNonBlocking :
        Error("Only socket descriptors can change their non-blocking mode on Windows")

    @HiddenFromObjC
    class SetStdHandle(cause: Throwable) : Error("SetStdHandle failed", cause)

    @HiddenFromObjC
    class Io(cause: Throwable) : Error("IoError", cause)
}


/**
 * Platform-independent interface returning a non-owning reference to the
 * underlying platform file descriptor.
 */
interface AsRawFileDescriptor {
    fun asRawFileDescriptor(): RawFileDescriptor
}

/**
 * Platform-independent interface converting an instance into the
 * underlying platform file descriptor.
 */
interface IntoRawFileDescriptor {
    fun intoRawFileDescriptor(): RawFileDescriptor
}

internal interface FromRawFileDescriptor<T> {
    fun fromRawFileDescriptor(fd: RawFileDescriptor): T
}

interface AsRawSocketDescriptor {
    fun asSocketDescriptor(): SocketDescriptor
}

interface IntoRawSocketDescriptor {
    fun intoSocketDescriptor(): SocketDescriptor
}

internal interface FromRawSocketDescriptor<T> {
    fun fromSocketDescriptor(fd: SocketDescriptor): T
}

/**
 * [OwnedHandle] manages the lifecycle of the platform [RawFileDescriptor] type.
 */
class OwnedHandle(
    var handle: RawFileDescriptor,
    var handleType: HandleType = HandleType.Unknown,
) : AsRawFileDescriptor, IntoRawFileDescriptor, AutoCloseable {

    private var isClosed = false

    override fun asRawFileDescriptor(): RawFileDescriptor = handle

    override fun intoRawFileDescriptor(): RawFileDescriptor {
        isClosed = true
        return handle
    }

    /**
     * Attempt to duplicate the underlying handle and return an [OwnedHandle]
     * wrapped around the duplicate.
     */
    fun tryClone(): Result<OwnedHandle> = dupImpl(this, handleType)

    override fun close() {
        if (!isClosed) {
            isClosed = true
            closeHandlePlatform(handle, handleType)
        }
    }

    companion object {
        /**
         * Create a new handle from an object convertible to [RawFileDescriptor].
         */
        fun <F : IntoRawFileDescriptor> new(f: F): OwnedHandle {
            val handle = f.intoRawFileDescriptor()
            return OwnedHandle(
                handle = handle,
                handleType = probeHandleType(handle),
            )
        }

        fun fromRaw(handle: RawFileDescriptor, handleType: HandleType = HandleType.Unknown): OwnedHandle {
            return OwnedHandle(
                handle = handle,
                handleType = if (handleType == HandleType.Unknown) probeHandleType(handle) else handleType,
            )
        }

        /**
         * Attempt to duplicate the underlying handle from an object representable
         * as [RawFileDescriptor].
         */
        fun <F : AsRawFileDescriptor> dup(f: F): Result<OwnedHandle> {
            return dupImpl(f, HandleType.Unknown)
        }
    }
}

/**
 * [FileDescriptor] is a wrapper on top of [OwnedHandle] exposing read and write operations.
 */
class FileDescriptor(
    val handle: OwnedHandle,
) : AsRawFileDescriptor, IntoRawFileDescriptor, AsRawSocketDescriptor, IntoRawSocketDescriptor, AutoCloseable {

    override fun asRawFileDescriptor(): RawFileDescriptor = handle.asRawFileDescriptor()

    override fun intoRawFileDescriptor(): RawFileDescriptor = handle.intoRawFileDescriptor()

    override fun asSocketDescriptor(): SocketDescriptor = handle.asRawFileDescriptor()

    override fun intoSocketDescriptor(): SocketDescriptor = handle.intoRawFileDescriptor()

    /**
     * Attempt to duplicate the underlying handle and return a [FileDescriptor]
     * wrapped around the duplicate.
     */
    fun tryClone(): Result<FileDescriptor> {
        return handle.tryClone().map { FileDescriptor(it) }
    }

    /**
     * Create a standard I/O representation for process redirection.
     */
    fun asStdio(): Result<Any?> {
        return asStdioPlatform(this)
    }

    /**
     * Create a file representation using a duplicated handle.
     */
    fun asFile(): Result<Any?> {
        return asFilePlatform(this)
    }

    /**
     * Attempt to change the non-blocking I/O mode.
     */
    fun setNonBlocking(nonBlocking: Boolean): Result<Unit> {
        return setNonBlockingPlatform(this, nonBlocking)
    }

    /**
     * Read bytes from the descriptor into [buf].
     */
    fun read(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Result<Int> {
        return readPlatform(handle.asRawFileDescriptor(), handle.handleType, buf, offset, length)
    }

    /**
     * Write bytes from [buf] to the descriptor.
     */
    fun write(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Result<Int> {
        return writePlatform(handle.asRawFileDescriptor(), handle.handleType, buf, offset, length)
    }

    /**
     * Flush buffered data.
     */
    fun flush(): Result<Unit> {
        return flushPlatform(handle.asRawFileDescriptor())
    }

    /**
     * Read all remaining bytes into a UTF-8 string.
     */
    fun readToString(): Result<String> {
        return readToStringPlatform(this)
    }

    override fun close() {
        handle.close()
    }

    companion object {
        /**
         * Create a new descriptor from an object convertible to [RawFileDescriptor].
         */
        fun <F : IntoRawFileDescriptor> new(f: F): FileDescriptor {
            val handle = OwnedHandle.new(f)
            return FileDescriptor(handle)
        }

        fun fromRaw(rawFd: RawFileDescriptor): FileDescriptor {
            return FileDescriptor(OwnedHandle.fromRaw(rawFd))
        }

        /**
         * Attempt to duplicate the underlying handle from an object representable
         * as [RawFileDescriptor].
         */
        fun <F : AsRawFileDescriptor> dup(f: F): Result<FileDescriptor> {
            return OwnedHandle.dup(f).map { FileDescriptor(it) }
        }

        /**
         * Duplicate descriptor to a specific target descriptor number.
         */
        fun <F : AsRawFileDescriptor> dup2(f: F, destFd: RawFileDescriptor): Result<FileDescriptor> {
            return dup2Platform(f, destFd).map { FileDescriptor(it) }
        }

        /**
         * Redirect standard I/O to the specified descriptor.
         */
        fun <F : AsRawFileDescriptor> redirectStdio(f: F, stdio: StdioDescriptor): Result<FileDescriptor> {
            return redirectStdioPlatform(f, stdio)
        }
    }
}

enum class StdioDescriptor {
    Stdin,
    Stdout,
    Stderr,
}

/**
 * Represents the readable and writable ends of a pipe.
 */
class Pipe(
    val read: FileDescriptor,
    val write: FileDescriptor,
) : AutoCloseable {

    override fun close() {
        try {
            read.close()
        } finally {
            write.close()
        }
    }

    companion object {
        /**
         * Create a new unidirectional pipe pair.
         */
        fun new(): Result<Pipe> = createPipePlatform()
    }
}

/**
 * Examines a set of file descriptors for readiness.
 */
fun poll(pfd: List<Pollfd>, duration: Duration? = null): Result<Int> {
    return pollPlatform(pfd, duration)
}

/**
 * Examines an array of file descriptors for readiness.
 */
fun poll(pfd: Array<Pollfd>, duration: Duration? = null): Result<Int> {
    return pollPlatform(pfd.toList(), duration)
}

/**
 * Create a pair of connected stream sockets.
 */
fun socketpair(): Result<Pair<FileDescriptor, FileDescriptor>> {
    return socketpairPlatform()
}
