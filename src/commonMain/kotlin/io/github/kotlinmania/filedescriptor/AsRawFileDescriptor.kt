// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

/**
 * Platform-independent interface returning a non-owning reference to the
 * underlying platform file descriptor.
 */
interface AsRawFileDescriptor {
    fun asRawFileDescriptor(): Long
}
