@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

// port-lint: source src/lib.rs
package io.github.kotlinmania.filedescriptor

import kotlin.native.HiddenFromObjC

/**
 * The purpose of this crate is to make it a bit more ergonomic for portable
 * applications that need to work with the platform level `RawFd` and
 * `RawHandle` types.
 *
 * Rather than conditionally using `RawFd` and `RawHandle`, the [FileDescriptor]
 * type can be used to manage ownership, duplicate, read and write.
 *
 * ## FileDescriptor
 *
 * This is a bit of a contrived example, but demonstrates how to avoid
 * the conditional code that would otherwise be required to deal with
 * calling `as_raw_fd` and `as_raw_handle`:
 *
 * ```
 * fun getStdout(): Result<FileDescriptor> {
 *     val stdout = systemStdout()
 *     return FileDescriptor.dup(stdout)
 * }
 *
 * fun printSomething(): Result<Unit> {
 *     return getStdout().mapCatching { it.write("hello".encodeToByteArray()) }
 * }
 * ```
 *
 * ## Pipe
 * The [Pipe] type makes it more convenient to create a pipe and manage
 * the lifetime of both the read and write ends of that pipe.
 *
 * ```
 * val pipe = Pipe.new().getOrThrow()
 * pipe.write.write("hello".encodeToByteArray())
 * pipe.write.close()
 *
 * val s = pipe.read.readToString()
 * check(s == "hello")
 * ```
 *
 * ## Socketpair
 * The [socketpair] function returns a pair of connected `SOCK_STREAM`
 * sockets and functions both on posix and windows systems.
 *
 * ```
 * val (a, b) = socketpair().getOrThrow()
 * a.write("hello".encodeToByteArray())
 * a.close()
 *
 * val s = b.readToString()
 * check(s == "hello")
 * ```
 *
 * ## Polling
 * The `mio` crate offers powerful and scalable IO multiplexing, but there
 * are some situations where `mio` doesn't fit.  The `filedescriptor` crate
 * offers a `poll(2)` compatible interface suitable for testing the readiness
 * of a set of file descriptors.  On unix systems this is a very thin wrapper
 * around `poll(2)`, except on macOS where it is actually a wrapper around
 * the `select(2)` interface.  On Windows systems the winsock `WSAPoll`
 * function is used instead.
 *
 * ```
 * val (a, b) = socketpair().getOrThrow()
 * val pollArray = mutableListOf(Pollfd(
 *     fd = a.asSocketDescriptor(),
 *     events = POLLIN,
 *     revents = 0,
 * ))
 * // sleeps for 20 milliseconds because `a` is not yet ready
 * check(poll(pollArray, Duration.parse("20ms")).getOrThrow() == 0)
 *
 * b.write("hello".encodeToByteArray())
 *
 * // Now a is ready for read
 * check(poll(pollArray, Duration.parse("20ms")).getOrThrow() == 1)
 * ```
 */

/**
 * `RawFileDescriptor` is a platform independent type alias for the
 * underlying platform file descriptor type.  It is primarily useful
 * for avoiding using platform-conditional blocks in platform independent code.
 *
 * The underlying width is wide enough to carry both a unix `RawFd` (signed
 * 32-bit integer) and a Windows `RawHandle` (pointer-sized integer).
 */
typealias RawFileDescriptor = Long

/**
 * `SocketDescriptor` is a platform independent type alias for the
 * underlying platform socket descriptor type.  It is primarily useful
 * for avoiding using platform-conditional blocks in platform independent code.
 */
typealias SocketDescriptor = Long

/**
 * Internal classifier carried by [OwnedHandle] to remember whether a handle
 * refers to a character device, disk file, pipe, socket, or other kernel
 * object. On unix this is always [Unknown] because the unix syscalls don't
 * need the distinction.
 */
internal enum class HandleType {
    Unknown,
    Char,
    Disk,
    Pipe,
    Socket,
}

internal fun defaultHandleType(): HandleType = HandleType.Unknown

internal fun probeHandleType(handle: RawFileDescriptor): HandleType = HandleType.Unknown

class Pollfd(
    var fd: SocketDescriptor,
    var events: Short,
    var revents: Short,
)

const val POLLIN: Short = 0x0001
const val POLLOUT: Short = 0x0004
const val POLLERR: Short = 0x0008
const val POLLHUP: Short = 0x0010

/**
 * Errors raised by [FileDescriptor], [OwnedHandle], [Pipe], [poll], and
 * [socketpair]. The variant naming and message formatting mirror the
 * upstream `thiserror`-derived `Error` enum.
 */
@HiddenFromObjC
sealed class Error(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    /** failed to create a pipe */
    @HiddenFromObjC
    class Pipe(cause: Throwable) : Error("failed to create a pipe", cause)

    /** failed to create a socketpair */
    @HiddenFromObjC
    class Socketpair(cause: Throwable) : Error("failed to create a socketpair", cause)

    /** failed to create a socket */
    @HiddenFromObjC
    class Socket(cause: Throwable) : Error("failed to create a socket", cause)

    /** failed to bind a socket */
    @HiddenFromObjC
    class Bind(cause: Throwable) : Error("failed to bind a socket", cause)

    /** failed to fetch socket name */
    @HiddenFromObjC
    class Getsockname(cause: Throwable) : Error("failed to fetch socket name", cause)

    /** failed to set socket to listen mode */
    @HiddenFromObjC
    class Listen(cause: Throwable) : Error("failed to set socket to listen mode", cause)

    /** failed to connect socket */
    @HiddenFromObjC
    class Connect(cause: Throwable) : Error("failed to connect socket", cause)

    /** failed to accept socket */
    @HiddenFromObjC
    class Accept(cause: Throwable) : Error("failed to accept socket", cause)

    /** fcntl read failed */
    @HiddenFromObjC
    class Fcntl(cause: Throwable) : Error("fcntl read failed", cause)

    /** failed to set cloexec */
    @HiddenFromObjC
    class Cloexec(cause: Throwable) : Error("failed to set cloexec", cause)

    /** failed to change non-blocking mode */
    @HiddenFromObjC
    class FionBio(cause: Throwable) : Error("failed to change non-blocking mode", cause)

    /** poll failed */
    @HiddenFromObjC
    class Poll(cause: Throwable) : Error("poll failed", cause)

    /** dup of fd `fd` failed */
    @HiddenFromObjC
    class Dup(val fd: Long, cause: Throwable) : Error("dup of fd $fd failed", cause)

    /** dup of fd `srcFd` to fd `destFd` failed */
    @HiddenFromObjC
    class Dup2(val srcFd: Long, val destFd: Long, cause: Throwable) :
        Error("dup of fd $srcFd to fd $destFd failed", cause)

    /** Illegal fd value */
    @HiddenFromObjC
    class IllegalFdValue(val fd: Long) : Error("Illegal fd value $fd")

    /** fd value too large to use with select(2) */
    @HiddenFromObjC
    class FdValueOutsideFdSetSize(val fd: Long) :
        Error("fd value $fd too large to use with select(2)")

    /** Only socket descriptors can change their non-blocking mode on Windows */
    @HiddenFromObjC
    object OnlySocketsNonBlocking :
        Error("Only socket descriptors can change their non-blocking mode on Windows")

    /** SetStdHandle failed */
    @HiddenFromObjC
    class SetStdHandle(cause: Throwable) : Error("SetStdHandle failed", cause)

    /** IoError */
    @HiddenFromObjC
    class Io(cause: Throwable) : Error("IoError", cause)
}

typealias Result<T> = kotlin.Result<T>

/**
 * `AsRawFileDescriptor` is a platform independent trait for returning
 * a non-owning reference to the underlying platform file descriptor
 * type.
 */
interface AsRawFileDescriptor {
    fun asRawFileDescriptor(): RawFileDescriptor
}

/**
 * `IntoRawFileDescriptor` is a platform independent trait for converting
 * an instance into the underlying platform file descriptor type.
 */
interface IntoRawFileDescriptor {
    fun intoRawFileDescriptor(): RawFileDescriptor
}

/**
 * `FromRawFileDescriptor` is a platform independent trait for creating
 * an instance from the underlying platform file descriptor type.
 * Because the platform file descriptor type has no inherent ownership
 * management, the [fromRawFileDescriptor] function takes the same care
 * the upstream `unsafe fn` documents: the caller must ensure that it
 * is used appropriately.
 */
// Marked `internal` so the Swift Export bridge does not emit an
// `Unchecked cast of 'Any?' to 'FromRawFileDescriptor<Any?>'` warning
// against the receiver of `fromRawFileDescriptor`. The interface has no
// in-tree implementers; downstream Kotlin callers that need it can be
// reintroduced behind a non-generic façade when they materialize.
internal interface FromRawFileDescriptor<T> {
    fun fromRawFileDescriptor(fd: RawFileDescriptor): T
}

interface AsRawSocketDescriptor {
    fun asSocketDescriptor(): SocketDescriptor
}

interface IntoRawSocketDescriptor {
    fun intoSocketDescriptor(): SocketDescriptor
}

// Same rationale as [FromRawFileDescriptor]: marked `internal` so the
// Swift Export bridge does not emit unchecked-cast warnings against the
// receiver.
internal interface FromRawSocketDescriptor<T> {
    fun fromSocketDescriptor(fd: SocketDescriptor): T
}

/**
 * [OwnedHandle] allows managing the lifetime of the platform `RawFileDescriptor`
 * type.  It is exposed in the interface of this crate primarily for convenience
 * on Windows where the system handle type is used for a variety of objects
 * that don't support reading and writing.
 */
class OwnedHandle internal constructor(
    internal var handle: RawFileDescriptor,
    internal var handleType: HandleType,
) : AsRawFileDescriptor, IntoRawFileDescriptor {
    override fun asRawFileDescriptor(): RawFileDescriptor = handle

    override fun intoRawFileDescriptor(): RawFileDescriptor = handle

    companion object {
        /**
         * Create a new handle from some object that is convertible into
         * the system `RawFileDescriptor` type.  This consumes the parameter
         * and replaces it with an [OwnedHandle] instance.
         */
        fun <F : IntoRawFileDescriptor> new(f: F): OwnedHandle {
            val handle = f.intoRawFileDescriptor()
            return OwnedHandle(
                handle = handle,
                handleType = probeHandleType(handle),
            )
        }
    }
}

/**
 * [FileDescriptor] is a thin wrapper on top of the [OwnedHandle] type that
 * exposes the ability to Read and Write to the platform `RawFileDescriptor`.
 *
 * This is a bit of a contrived example, but demonstrates how to avoid
 * the conditional code that would otherwise be required to deal with
 * calling `as_raw_fd` and `as_raw_handle`:
 *
 * ```
 * fun getStdout(): Result<FileDescriptor> {
 *     val stdout = systemStdout()
 *     return FileDescriptor.dup(stdout)
 * }
 *
 * fun printSomething(): Result<Unit> {
 *     return getStdout().mapCatching { it.write("hello".encodeToByteArray()) }
 * }
 * ```
 */
class FileDescriptor internal constructor(
    internal val handle: OwnedHandle,
) : AsRawFileDescriptor, IntoRawFileDescriptor {
    override fun asRawFileDescriptor(): RawFileDescriptor = handle.asRawFileDescriptor()

    override fun intoRawFileDescriptor(): RawFileDescriptor = handle.intoRawFileDescriptor()

    companion object {
        /**
         * Create a new descriptor from some object that is convertible into
         * the system `RawFileDescriptor` type.  This consumes the parameter
         * and replaces it with a [FileDescriptor] instance.
         */
        fun <F : IntoRawFileDescriptor> new(f: F): FileDescriptor {
            val handle = OwnedHandle.new(f)
            return FileDescriptor(handle)
        }
    }
}

enum class StdioDescriptor {
    Stdin,
    Stdout,
    Stderr,
}

/**
 * Represents the readable and writable ends of a pair of descriptors
 * connected via a kernel pipe.
 *
 * ```
 * val pipe = Pipe.new().getOrThrow()
 * pipe.write.write("hello".encodeToByteArray())
 * pipe.write.close()
 *
 * val s = pipe.read.readToString()
 * check(s == "hello")
 * ```
 */
class Pipe(
    /** The readable end of the pipe */
    val read: FileDescriptor,
    /** The writable end of the pipe */
    val write: FileDescriptor,
)

