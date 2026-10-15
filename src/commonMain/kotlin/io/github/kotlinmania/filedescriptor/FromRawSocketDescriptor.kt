@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

internal interface FromRawSocketDescriptor<T> {
    fun fromSocketDescriptor(fd: Long): T
}
