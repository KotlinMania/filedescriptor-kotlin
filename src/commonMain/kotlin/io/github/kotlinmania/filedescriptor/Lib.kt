// port-lint: source src/lib.rs
package io.github.kotlinmania.filedescriptor

import kotlin.time.Duration

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
 */
expect class RawFileDescriptor

/**
 * `SocketDescriptor` is a platform independent type alias for the
 * underlying platform socket descriptor type.  It is primarily useful
 * for avoiding using platform-conditional blocks in platform independent code.
 */
expect class SocketDescriptor

internal expect class HandleType

internal expect fun defaultHandleType(): HandleType

internal expect fun probeHandleType(handle: RawFileDescriptor): HandleType

expect class Pollfd(fd: SocketDescriptor, events: Short, revents: Short) {
    var fd: SocketDescriptor
    var events: Short
    var revents: Short
}

expect val POLLIN: Short
expect val POLLOUT: Short
expect val POLLERR: Short
expect val POLLHUP: Short

/**
 * Errors raised by [FileDescriptor], [OwnedHandle], [Pipe], [poll], and
 * [socketpair]. The variant naming and message formatting mirror the
 * upstream `thiserror`-derived `Error` enum.
 */
sealed class Error(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    /** failed to create a pipe */
    class Pipe(cause: Throwable) : Error("failed to create a pipe", cause)

    /** failed to create a socketpair */
    class Socketpair(cause: Throwable) : Error("failed to create a socketpair", cause)

    /** failed to create a socket */
    class Socket(cause: Throwable) : Error("failed to create a socket", cause)

    /** failed to bind a socket */
    class Bind(cause: Throwable) : Error("failed to bind a socket", cause)

    /** failed to fetch socket name */
    class Getsockname(cause: Throwable) : Error("failed to fetch socket name", cause)

    /** failed to set socket to listen mode */
    class Listen(cause: Throwable) : Error("failed to set socket to listen mode", cause)

    /** failed to connect socket */
    class Connect(cause: Throwable) : Error("failed to connect socket", cause)

    /** failed to accept socket */
    class Accept(cause: Throwable) : Error("failed to accept socket", cause)

    /** fcntl read failed */
    class Fcntl(cause: Throwable) : Error("fcntl read failed", cause)

    /** failed to set cloexec */
    class Cloexec(cause: Throwable) : Error("failed to set cloexec", cause)

    /** failed to change non-blocking mode */
    class FionBio(cause: Throwable) : Error("failed to change non-blocking mode", cause)

    /** poll failed */
    class Poll(cause: Throwable) : Error("poll failed", cause)

    /** dup of fd `fd` failed */
    class Dup(val fd: Long, cause: Throwable) : Error("dup of fd $fd failed", cause)

    /** dup of fd `srcFd` to fd `destFd` failed */
    class Dup2(val srcFd: Long, val destFd: Long, cause: Throwable) :
        Error("dup of fd $srcFd to fd $destFd failed", cause)

    /** Illegal fd value */
    class IllegalFdValue(val fd: Long) : Error("Illegal fd value $fd")

    /** fd value too large to use with select(2) */
    class FdValueOutsideFdSetSize(val fd: Long) :
        Error("fd value $fd too large to use with select(2)")

    /** Only socket descriptors can change their non-blocking mode on Windows */
    object OnlySocketsNonBlocking :
        Error("Only socket descriptors can change their non-blocking mode on Windows")

    /** SetStdHandle failed */
    class SetStdHandle(cause: Throwable) : Error("SetStdHandle failed", cause)

    /** IoError */
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
interface FromRawFileDescriptor<T> {
    fun fromRawFileDescriptor(fd: RawFileDescriptor): T
}

interface AsRawSocketDescriptor {
    fun asSocketDescriptor(): SocketDescriptor
}

interface IntoRawSocketDescriptor {
    fun intoSocketDescriptor(): SocketDescriptor
}

interface FromRawSocketDescriptor<T> {
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
) {
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

        /**
         * Attempt to duplicate the underlying handle from an object that is
         * representable as the system `RawFileDescriptor` type and return an
         * [OwnedHandle] wrapped around the duplicate.  Since the duplication
         * requires kernel resources that may not be available, this is a
         * potentially fallible operation.
         * The returned handle has a separate lifetime from the source, but
         * references the same object at the kernel level.
         */
        fun <F : AsRawFileDescriptor> dup(f: F): Result<OwnedHandle> {
            return ownedHandleDupImpl(f, defaultHandleType())
        }
    }

    /**
     * Attempt to duplicate the underlying handle and return an
     * [OwnedHandle] wrapped around the duplicate.  Since the duplication
     * requires kernel resources that may not be available, this is a
     * potentially fallible operation.
     * The returned handle has a separate lifetime from the source, but
     * references the same object at the kernel level.
     */
    fun tryClone(): Result<OwnedHandle> {
        return ownedHandleDupImpl(this, this.handleType)
    }
}

/**
 * Platform-specific duplication primitive used by [OwnedHandle.dup] and
 * [OwnedHandle.tryClone]. Defined per-target by `unix.rs` / `windows.rs`.
 */
internal expect fun <F : AsRawFileDescriptor> ownedHandleDupImpl(
    f: F,
    handleType: HandleType,
): Result<OwnedHandle>

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
) {
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

        /**
         * Attempt to duplicate the underlying handle from an object that is
         * representable as the system `RawFileDescriptor` type and return a
         * [FileDescriptor] wrapped around the duplicate.  Since the duplication
         * requires kernel resources that may not be available, this is a
         * potentially fallible operation.
         * The returned handle has a separate lifetime from the source, but
         * references the same object at the kernel level.
         */
        fun <F : AsRawFileDescriptor> dup(f: F): Result<FileDescriptor> {
            return OwnedHandle.dup(f).map { handle -> FileDescriptor(handle) }
        }

        /**
         * Attempt to redirect stdio to the underlying handle and return
         * a [FileDescriptor] wrapped around the original stdio source.
         * Since the redirection requires kernel resources that may not be
         * available, this is a potentially fallible operation.
         * Supports stdin, stdout, and stderr redirections.
         */
        fun <F : AsRawFileDescriptor> redirectStdio(
            f: F,
            stdio: StdioDescriptor,
        ): Result<FileDescriptor> {
            return fileDescriptorRedirectStdioImpl(f, stdio)
        }
    }

    /**
     * Attempt to duplicate the underlying handle and return a
     * [FileDescriptor] wrapped around the duplicate.  Since the duplication
     * requires kernel resources that may not be available, this is a
     * potentially fallible operation.
     * The returned handle has a separate lifetime from the source, but
     * references the same object at the kernel level.
     */
    fun tryClone(): Result<FileDescriptor> {
        return handle.tryClone().map { dup -> FileDescriptor(dup) }
    }

    /**
     * A convenience method for creating a [Stdio] object
     * to be used for eg: redirecting the stdio streams of a child
     * process.  The `Stdio` is created using a duplicated handle so
     * that the source handle remains alive.
     */
    fun asStdio(): Result<Stdio> = asStdioImpl()

    /**
     * A convenience method for creating a [PlatformFile] object,
     * the kotlin-port equivalent of `std::fs::File`.
     * The file is created using a duplicated handle so
     * that the source handle remains alive.
     */
    fun asFile(): Result<PlatformFile> = asFileImpl()

    /**
     * Attempt to change the non-blocking IO mode of the file descriptor.
     * Not all kinds of file descriptor can be placed in non-blocking mode
     * on all systems, and some file descriptors will claim to be in
     * non-blocking mode but it will have no effect.
     * File descriptors based on sockets are the most portable type
     * that can be successfully made non-blocking.
     */
    fun setNonBlocking(nonBlocking: Boolean): Result<Unit> = setNonBlockingImpl(nonBlocking)
}

/**
 * Platform analog of `std::process::Stdio`. Defined per-target by `unix.rs` /
 * `windows.rs` and threaded back to the standard child-process spawning APIs
 * on each platform.
 */
expect class Stdio

/** Platform analog of `std::fs::File`, returned by [FileDescriptor.asFile]. */
expect class PlatformFile

internal expect fun FileDescriptor.asStdioImpl(): Result<Stdio>

internal expect fun FileDescriptor.asFileImpl(): Result<PlatformFile>

internal expect fun FileDescriptor.setNonBlockingImpl(nonBlocking: Boolean): Result<Unit>

internal expect fun <F : AsRawFileDescriptor> fileDescriptorRedirectStdioImpl(
    f: F,
    stdio: StdioDescriptor,
): Result<FileDescriptor>

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

/**
 * Examines a set of FileDescriptors to see if some of them are ready for I/O,
 * or if certain events have occurred on them.
 *
 * This uses the system native readiness checking mechanism, which on Windows
 * means that it does NOT use IOCP and that this only works with sockets on
 * Windows.  If you need IOCP then the `mio` crate is recommended for a much
 * more scalable solution.
 *
 * On macOS, the `poll(2)` implementation has problems when used with eg: pty
 * descriptors, so this implementation of poll uses the `select(2)` interface
 * under the covers.  That places a limit on the maximum file descriptor value
 * that can be passed to poll.  If a file descriptor is out of range then an
 * error will returned.  This limitation could potentially be lifted in the
 * future.
 *
 * On Windows, `WSAPoll` is used to implement readiness checking, which has
 * the consequence that it can only be used with sockets.
 *
 * If [duration] is `null`, then [poll] will block until any of the requested
 * events are ready.  Otherwise, [duration] specifies how long to wait for
 * readiness before giving up.
 *
 * The return value is the number of entries that were satisfied; `0` means
 * that none were ready after waiting for the specified duration.
 *
 * The [pfd] array is mutated and the `revents` field is updated to indicate
 * which of the events were received.
 */
fun poll(pfd: MutableList<Pollfd>, duration: Duration?): Result<Int> = pollImpl(pfd, duration)

internal expect fun pollImpl(pfd: MutableList<Pollfd>, duration: Duration?): Result<Int>

/**
 * Create a pair of connected sockets.
 *
 * This implementation creates a pair of `SOCK_STREAM` sockets.
 */
fun socketpair(): Result<Pair<FileDescriptor, FileDescriptor>> = socketpairImpl()

internal expect fun socketpairImpl(): Result<Pair<FileDescriptor, FileDescriptor>>
