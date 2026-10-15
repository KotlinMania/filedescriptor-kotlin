// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

fun probeHandleType(handle: Long): HandleType = probeHandleTypePlatform(handle)
