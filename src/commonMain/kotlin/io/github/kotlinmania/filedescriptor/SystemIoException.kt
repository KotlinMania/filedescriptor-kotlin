// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

import kotlin.native.HiddenFromObjC

/** A captured system I/O failure, including whether the operation was interrupted. */
@HiddenFromObjC
class SystemIoException(
    val errorCode: Int,
    val interrupted: Boolean,
    message: String,
) : Exception(message)
