// port-lint: source unix.rs
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.kotlinmania.filedescriptor

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import libc.cinterop.libc_pipe

internal actual val atomicCloseOnExec: Boolean = false

internal actual fun createRawPipe(fds: CPointer<IntVar>): Int = libc_pipe(fds)

internal actual fun duplicatePosix(fd: Int, destFd: Int): Result<OwnedHandle> =
    nonAtomicDup2(fd.toLong(), destFd.toLong())
