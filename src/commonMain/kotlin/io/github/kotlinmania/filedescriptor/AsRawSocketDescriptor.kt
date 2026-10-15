@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

interface AsRawSocketDescriptor {
    fun asSocketDescriptor(): Long
}
