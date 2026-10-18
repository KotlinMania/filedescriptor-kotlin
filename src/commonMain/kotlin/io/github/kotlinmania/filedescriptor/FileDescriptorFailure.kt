package io.github.kotlinmania.filedescriptor

import io.github.kotlinmania.thiserror.StdError

/**
 * Errors raised by [FileDescriptor], [OwnedHandle], [Pipe], [poll], and
 * [socketpair].
 */
internal sealed class FileDescriptorFailure(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause),
    StdError {
    override fun source(): StdError? = cause as? StdError

    class Pipe(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to create a pipe", cause)

    class Socketpair(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to create a socketpair", cause)

    class Socket(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to create a socket", cause)

    class Bind(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to bind a socket", cause)

    class Getsockname(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to fetch socket name", cause)

    class Listen(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to set socket to listen mode", cause)

    class Connect(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to connect socket", cause)

    class Accept(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to accept socket", cause)

    class Fcntl(
        cause: Throwable,
    ) : FileDescriptorFailure("fcntl read failed", cause)

    class Cloexec(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to set cloexec", cause)

    class FionBio(
        cause: Throwable,
    ) : FileDescriptorFailure("failed to change non-blocking mode", cause)

    class Poll(
        cause: Throwable,
    ) : FileDescriptorFailure("poll failed", cause)

    class Dup(
        val fd: Long,
        cause: Throwable,
    ) : FileDescriptorFailure("dup of fd $fd failed", cause)

    class Dup2(
        val srcFd: Long,
        val destFd: Long,
        cause: Throwable,
    ) : FileDescriptorFailure("dup of fd $srcFd to fd $destFd failed", cause)

    class IllegalFdValue(
        val fd: Long,
    ) : FileDescriptorFailure("Illegal fd value $fd")

    class FdValueOutsideFdSetSize(
        val fd: Long,
    ) : FileDescriptorFailure("fd value $fd too large to use with select(2)")

    data object OnlySocketsNonBlocking :
        FileDescriptorFailure("Only socket descriptors can change their non-blocking mode on Windows")

    class SetStdHandle(
        cause: Throwable,
    ) : FileDescriptorFailure("SetStdHandle failed", cause)

    class Io(
        cause: Throwable,
    ) : FileDescriptorFailure("IoError", cause)
}
