// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

/**
 * Create a pair of connected stream sockets.
 */
fun socketpair(): Result<Pair<FileDescriptor, FileDescriptor>> = socketpairPlatform()
