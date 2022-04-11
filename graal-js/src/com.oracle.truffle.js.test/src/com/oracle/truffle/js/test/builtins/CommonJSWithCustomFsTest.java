/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.test.interop.AsyncInteropTest.Thenable;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommonJSWithCustomFsTest {

    @Test
    public void testDeniedSync() {
        String src = "(function() {" +
                        "  try {" +
                        "    require('does.not.exist');" +
                        "  } catch(e) {" +
                        "    return 'got exception: ' + e;" +
                        "  }" +
                        "})";
        DenyAllFs fs = new DenyAllFs();
        Context ctx = createTestContext(fs, "./");
        fs.denyAll();
        Value doesCatch = ctx.eval(Source.create("js", src));
        Value execute = doesCatch.execute();
        assertEquals("got exception: TypeError: Cannot load module: 'does.not.exist': Not allowed by this FileSystem.", execute.asString());
    }

    @Test
    public void testDeniedAsync() {
        String js = "async function asyncFun(thenable) {" +
                        "  await thenable();" +
                        "  try {" +
                        "    require('does.not.exist');" +
                        "  } catch (e) {" +
                        "    return 'got exception: ' + e;" +
                        "  }" +
                        "}";
        DenyAllFs fs = new DenyAllFs();
        Context ctx = createTestContext(fs, "./");
        ctx.eval(Source.create("js", js));
        fs.denyAll();
        AtomicInteger executed = new AtomicInteger(0);
        Thenable thenable = (onResolve, onReject) -> onResolve.executeVoid(42);
        Consumer<Object> javaReaction = (value) -> {
            executed.incrementAndGet();
            assertEquals("got exception: TypeError: Cannot load CommonJS module: 'does.not.exist': Not allowed by this FileSystem.", value.toString());
        };
        ctx.getBindings("js").getMember("asyncFun").execute(thenable).invokeMember("then", javaReaction).invokeMember("catch", javaReaction);
        assertEquals(1, executed.get());
    }

    @Test
    public void testDeniedInit() {
        DenyAllFs fs = new DenyAllFs("./fail");
        Context ctx = createTestContext(fs, "./fail");
        fs.denyAll();
        try {
            ctx.eval(Source.create("js", "'should not eval!';"));
            assert false : "Should throw";
        } catch (PolyglotException e) {
            String message = e.getMessage();
            assertEquals("Not allowed by this FileSystem.", message);
            assertTrue(e.isHostException());
            assertTrue(e.asHostException() instanceof SecurityException);
        }
    }

    private static class DenyAllFs implements FileSystem {

        private final String denyPath;
        private final FileSystem delegate = FileSystem.newDefaultFileSystem();
        private boolean blockAll = false;

        DenyAllFs(String blockPath) {
            this.denyPath = blockPath;
        }

        DenyAllFs() {
            this(null);
        }

        public void denyAll() {
            blockAll = true;
        }

        private void checkAccess() {
            if (blockAll) {
                throw new SecurityException("Not allowed by this FileSystem.");
            }
        }

        @Override
        public Path parsePath(final URI uri) {
            return delegate.parsePath(uri);
        }

        @Override
        public Path parsePath(final String path) {
            if (denyPath != null && denyPath.equals(path)) {
                checkAccess();
            }
            return delegate.parsePath(path);
        }

        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
            if (!path.toString().isEmpty()) {
                checkAccess();
            }
            delegate.checkAccess(path, modes, linkOptions);
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
            checkAccess();
            delegate.createDirectory(dir, attrs);
        }

        @Override
        public void delete(Path path) throws IOException {
            checkAccess();
            delegate.delete(path);
        }

        @Override
        public void copy(Path source, Path target, CopyOption... options) throws IOException {
            checkAccess();
            delegate.copy(source, target, options);
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) throws IOException {
            checkAccess();
            delegate.move(source, target, options);
        }

        @Override
        public SeekableByteChannel newByteChannel(Path inPath, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
            checkAccess();
            return delegate.newByteChannel(inPath, options, attrs);
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
            checkAccess();
            return delegate.newDirectoryStream(dir, filter);
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
            checkAccess();
            return delegate.readAttributes(path, attributes, options);
        }

        @Override
        public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
            checkAccess();
            delegate.setAttribute(path, attribute, value, options);
        }

        @Override
        public Path toAbsolutePath(Path path) {
            checkAccess();
            return delegate.toAbsolutePath(path);
        }

        @Override
        public void setCurrentWorkingDirectory(Path currentWorkingDirectory) {
            checkAccess();
            delegate.setCurrentWorkingDirectory(currentWorkingDirectory);
        }

        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
            checkAccess();
            return delegate.toRealPath(path, linkOptions);
        }

        @Override
        public Path getTempDirectory() {
            checkAccess();
            return delegate.getTempDirectory();
        }

        @Override
        public void createLink(Path link, Path existing) throws IOException {
            checkAccess();
            delegate.createLink(link, existing);
        }

        @Override
        public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
            checkAccess();
            delegate.createSymbolicLink(link, target, attrs);
        }

        @Override
        public Path readSymbolicLink(Path link) throws IOException {
            checkAccess();
            return delegate.readSymbolicLink(link);
        }

        @Override
        public boolean isSameFile(Path path1, Path path2, LinkOption... options) throws IOException {
            checkAccess();
            return delegate.isSameFile(path1, path2, options);
        }
    }

    private static Context createTestContext(FileSystem fs, String cwd) {
        final Map<String, String> options = new HashMap<>();
        options.put("js.commonjs-require", "true");
        options.put("js.commonjs-require-cwd", cwd);

        return Context.newBuilder("js").allowExperimentalOptions(true).allowHostAccess(HostAccess.ALL).options(options).allowIO(true).fileSystem(fs).build();
    }

}
