// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

/**
 * Internal classifier carried by [OwnedHandle] to record handle category.
 */
enum class HandleType {
    Char,
    Disk,
    Pipe,
    Socket,
    Unknown,
}
