@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

internal interface FromRawFileDescriptor<T> {
    fun fromRawFileDescriptor(fd: Long): T
}
