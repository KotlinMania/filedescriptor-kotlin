package io.github.kotlinmania.filedescriptor

/** Failure details for descriptor, pipe, socket-pair, and polling operations. */
class FileDescriptorError private constructor(
    val kind: FileDescriptorErrorKind,
    val message: String,
    val systemError: SystemIoError?,
    val sourceDescriptor: Long?,
    val destinationDescriptor: Long?,
) {
    companion object {
        internal fun fromFailure(failure: Throwable): FileDescriptorError {
            if (failure is FileDescriptorException) return failure.error
            val kind =
                when (failure) {
                    is FileDescriptorFailure.Pipe -> FileDescriptorErrorKind.Pipe
                    is FileDescriptorFailure.Socketpair -> FileDescriptorErrorKind.Socketpair
                    is FileDescriptorFailure.Socket -> FileDescriptorErrorKind.Socket
                    is FileDescriptorFailure.Bind -> FileDescriptorErrorKind.Bind
                    is FileDescriptorFailure.Getsockname -> FileDescriptorErrorKind.Getsockname
                    is FileDescriptorFailure.Listen -> FileDescriptorErrorKind.Listen
                    is FileDescriptorFailure.Connect -> FileDescriptorErrorKind.Connect
                    is FileDescriptorFailure.Accept -> FileDescriptorErrorKind.Accept
                    is FileDescriptorFailure.Fcntl -> FileDescriptorErrorKind.Fcntl
                    is FileDescriptorFailure.Cloexec -> FileDescriptorErrorKind.Cloexec
                    is FileDescriptorFailure.FionBio -> FileDescriptorErrorKind.FionBio
                    is FileDescriptorFailure.Poll -> FileDescriptorErrorKind.Poll
                    is FileDescriptorFailure.Dup -> FileDescriptorErrorKind.Dup
                    is FileDescriptorFailure.Dup2 -> FileDescriptorErrorKind.Dup2
                    is FileDescriptorFailure.IllegalFdValue -> FileDescriptorErrorKind.IllegalFdValue
                    is FileDescriptorFailure.FdValueOutsideFdSetSize -> FileDescriptorErrorKind.FdValueOutsideFdSetSize
                    is FileDescriptorFailure.OnlySocketsNonBlocking -> FileDescriptorErrorKind.OnlySocketsNonBlocking
                    is FileDescriptorFailure.SetStdHandle -> FileDescriptorErrorKind.SetStdHandle
                    is FileDescriptorFailure.Io -> FileDescriptorErrorKind.Io
                    is IllegalArgumentException -> FileDescriptorErrorKind.InvalidArgument
                    else -> FileDescriptorErrorKind.Io
                }
            val system =
                when (failure) {
                    is SystemIoException -> failure
                    is FileDescriptorFailure -> failure.source() as? SystemIoException
                    else -> failure.cause as? SystemIoException
                }
            val source =
                when (failure) {
                    is FileDescriptorFailure.Dup -> failure.fd
                    is FileDescriptorFailure.Dup2 -> failure.srcFd
                    is FileDescriptorFailure.IllegalFdValue -> failure.fd
                    is FileDescriptorFailure.FdValueOutsideFdSetSize -> failure.fd
                    else -> null
                }
            return FileDescriptorError(
                kind,
                failure.message ?: kind.name,
                system?.let { SystemIoError(it.errorCode, it.interrupted, it.message.orEmpty()) },
                source,
                (failure as? FileDescriptorFailure.Dup2)?.destFd,
            )
        }
    }
}
