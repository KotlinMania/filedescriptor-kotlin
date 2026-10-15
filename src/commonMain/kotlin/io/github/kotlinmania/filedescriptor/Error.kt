// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

import kotlin.native.HiddenFromObjC

/**
 * Errors raised by [FileDescriptor], [OwnedHandle], [Pipe], [poll], and
 * [socketpair].
 */
@HiddenFromObjC
sealed class Error(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    @HiddenFromObjC
    class Pipe(
        cause: Throwable,
    ) : Error("failed to create a pipe", cause)

    @HiddenFromObjC
    class Socketpair(
        cause: Throwable,
    ) : Error("failed to create a socketpair", cause)

    @HiddenFromObjC
    class Socket(
        cause: Throwable,
    ) : Error("failed to create a socket", cause)

    @HiddenFromObjC
    class Bind(
        cause: Throwable,
    ) : Error("failed to bind a socket", cause)

    @HiddenFromObjC
    class Getsockname(
        cause: Throwable,
    ) : Error("failed to fetch socket name", cause)

    @HiddenFromObjC
    class Listen(
        cause: Throwable,
    ) : Error("failed to set socket to listen mode", cause)

    @HiddenFromObjC
    class Connect(
        cause: Throwable,
    ) : Error("failed to connect socket", cause)

    @HiddenFromObjC
    class Accept(
        cause: Throwable,
    ) : Error("failed to accept socket", cause)

    @HiddenFromObjC
    class Fcntl(
        cause: Throwable,
    ) : Error("fcntl read failed", cause)

    @HiddenFromObjC
    class Cloexec(
        cause: Throwable,
    ) : Error("failed to set cloexec", cause)

    @HiddenFromObjC
    class FionBio(
        cause: Throwable,
    ) : Error("failed to change non-blocking mode", cause)

    @HiddenFromObjC
    class Poll(
        cause: Throwable,
    ) : Error("poll failed", cause)

    @HiddenFromObjC
    class Dup(
        val fd: Long,
        cause: Throwable,
    ) : Error("dup of fd $fd failed", cause)

    @HiddenFromObjC
    class Dup2(
        val srcFd: Long,
        val destFd: Long,
        cause: Throwable,
    ) : Error("dup of fd $srcFd to fd $destFd failed", cause)

    @HiddenFromObjC
    class IllegalFdValue(
        val fd: Long,
    ) : Error("Illegal fd value $fd")

    @HiddenFromObjC
    class FdValueOutsideFdSetSize(
        val fd: Long,
    ) : Error("fd value $fd too large to use with select(2)")

    @HiddenFromObjC
    data object OnlySocketsNonBlocking :
        Error("Only socket descriptors can change their non-blocking mode on Windows")

    @HiddenFromObjC
    class SetStdHandle(
        cause: Throwable,
    ) : Error("SetStdHandle failed", cause)

    @HiddenFromObjC
    class Io(
        cause: Throwable,
    ) : Error("IoError", cause)
}
