// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

interface IntoRawSocketDescriptor {
    fun intoSocketDescriptor(): Long
}
