@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

/**
 * Platform-independent interface converting an instance into the
 * underlying platform file descriptor.
 */
interface IntoRawFileDescriptor {
    fun intoRawFileDescriptor(): Long
}
