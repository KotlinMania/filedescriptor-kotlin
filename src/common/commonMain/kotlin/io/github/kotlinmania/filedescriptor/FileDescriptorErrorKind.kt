package io.github.kotlinmania.filedescriptor

/** The operation or descriptor constraint that failed. */
enum class FileDescriptorErrorKind {
    Pipe,
    Socketpair,
    Socket,
    Bind,
    Getsockname,
    Listen,
    Connect,
    Accept,
    Fcntl,
    Cloexec,
    FionBio,
    Poll,
    Dup,
    Dup2,
    IllegalFdValue,
    FdValueOutsideFdSetSize,
    OnlySocketsNonBlocking,
    SetStdHandle,
    Io,
    InvalidArgument,
}
