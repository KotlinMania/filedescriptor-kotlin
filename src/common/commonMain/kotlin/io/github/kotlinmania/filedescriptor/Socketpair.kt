package io.github.kotlinmania.filedescriptor

/**
 * Create a pair of connected stream sockets.
 */
fun socketpair(): SocketPairResult = SocketPairResult.from(socketpairPlatform())
