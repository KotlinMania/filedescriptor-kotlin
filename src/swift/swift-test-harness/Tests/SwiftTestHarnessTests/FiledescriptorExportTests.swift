import Testing
import Filedescriptor

@Suite struct FiledescriptorExportTests {
    @Test func concretePipeResultsAndSharedDuration() throws {
        let created = Pipe.Companion.shared.new()
        #expect(created.error == nil)
        let pipe = try #require(created.value)
        defer { pipe.close() }
        let cloned = pipe.read.tryClone()
        let descriptor = try #require(cloned.value)
        defer { descriptor.close() }
        #expect(cloned.error == nil)
        #expect(descriptor.asRawFileDescriptor() != pipe.read.asRawFileDescriptor())
        let file = try #require(pipe.read.asFile().value)
        defer { file.close() }
        let stdio = try #require(pipe.read.asStdio().value)
        defer { stdio.close() }
        let zero = ExportedKotlinPackages.io.github.kotlinmania.libc.time.StdDuration.Companion.shared.ZERO
        let fd = Pollfd(fd: pipe.write.asRawFileDescriptor(), events: POLLOUT, revents: 0)
        let ready = poll(pfd: [fd], duration: zero)
        #expect(ready.value == 1)
        #expect(ready.error == nil)
        #expect(fd.revents & POLLOUT != 0)
    }

    @Test func concreteFailurePreservesDescriptor() throws {
        let zero = ExportedKotlinPackages.io.github.kotlinmania.libc.time.StdDuration.Companion.shared.ZERO
        let bad = Pollfd(fd: Int64.max, events: POLLIN, revents: 0)
        let result = poll(pfd: [bad], duration: zero)
        #expect(result.value == nil)
        let error = try #require(result.error)
        #expect(error.sourceDescriptor == Int64.max)
        #expect(!error.message.isEmpty)
    }
}
