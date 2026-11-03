package io.github.kotlinmania.filedescriptor

/**
 * Errors raised by [FileDescriptor], [OwnedHandle], [Pipe], [poll], and
 * [socketpair].
 */
sealed class FileDescriptorError(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class Pipe(
        cause: Throwable,
    ) : FileDescriptorError("failed to create a pipe", cause)

    class Socketpair(
        cause: Throwable,
    ) : FileDescriptorError("failed to create a socketpair", cause)

    class Socket(
        cause: Throwable,
    ) : FileDescriptorError("failed to create a socket", cause)

    class Bind(
        cause: Throwable,
    ) : FileDescriptorError("failed to bind a socket", cause)

    class Getsockname(
        cause: Throwable,
    ) : FileDescriptorError("failed to fetch socket name", cause)

    class Listen(
        cause: Throwable,
    ) : FileDescriptorError("failed to set socket to listen mode", cause)

    class Connect(
        cause: Throwable,
    ) : FileDescriptorError("failed to connect socket", cause)

    class Accept(
        cause: Throwable,
    ) : FileDescriptorError("failed to accept socket", cause)

    class Fcntl(
        cause: Throwable,
    ) : FileDescriptorError("fcntl read failed", cause)

    class Cloexec(
        cause: Throwable,
    ) : FileDescriptorError("failed to set cloexec", cause)

    class FionBio(
        cause: Throwable,
    ) : FileDescriptorError("failed to change non-blocking mode", cause)

    class Poll(
        cause: Throwable,
    ) : FileDescriptorError("poll failed", cause)

    class Dup(
        val fd: Long,
        cause: Throwable,
    ) : FileDescriptorError("dup of fd $fd failed", cause)

    class Dup2(
        val srcFd: Long,
        val destFd: Long,
        cause: Throwable,
    ) : FileDescriptorError("dup of fd $srcFd to fd $destFd failed", cause)

    class IllegalFdValue(
        val fd: Long,
    ) : FileDescriptorError("Illegal fd value $fd")

    class FdValueOutsideFdSetSize(
        val fd: Long,
    ) : FileDescriptorError("fd value $fd too large to use with select(2)")

    data object OnlySocketsNonBlocking :
        FileDescriptorError("Only socket descriptors can change their non-blocking mode on Windows")

    class SetStdHandle(
        cause: Throwable,
    ) : FileDescriptorError("SetStdHandle failed", cause)

    class Io(
        cause: Throwable,
    ) : FileDescriptorError("IoError", cause)
}
