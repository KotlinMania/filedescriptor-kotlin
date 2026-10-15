@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.filedescriptor

class Pollfd(
    var fd: Long,
    var events: Short,
    var revents: Short = 0,
)
