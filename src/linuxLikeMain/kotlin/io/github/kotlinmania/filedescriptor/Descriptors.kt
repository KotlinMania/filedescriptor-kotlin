// port-lint: source unix.rs
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.kotlinmania.filedescriptor

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import libc.cinterop.libc_dup3
import libc.cinterop.libc_pipe2

internal actual val atomicCloseOnExec: Boolean = true

internal actual fun createRawPipe(fds: CPointer<IntVar>): Int = libc_pipe2(fds, constants.o_cloexec)

internal actual fun duplicatePosix(fd: Int, destFd: Int): Result<OwnedHandle> =
    runCatching {
        val duplicate = libc_dup3(fd, destFd, constants.o_cloexec)
        if (duplicate == -1) {
            val error = systemError()
            if (error.errorCode == constants.einval) {
                nonAtomicDup2(fd.toLong(), destFd.toLong()).getOrThrow()
            } else {
                throw Error.Dup2(fd.toLong(), destFd.toLong(), error)
            }
        } else {
            OwnedHandle(duplicate.toLong())
        }
    }
