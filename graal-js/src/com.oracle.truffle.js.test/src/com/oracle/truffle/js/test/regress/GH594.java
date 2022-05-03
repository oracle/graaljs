/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.regress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.FileSystem;
import org.junit.Assert;
import org.junit.Test;

public class GH594 {

    @Test
    public void testGitHub594() throws IOException {
        Map<Path, String> files = new HashMap<>();
        files.put(Paths.get("/a/b/cjs-module.js").toAbsolutePath(), "exports.fooPromise = import('./esm-module.js').then(mod => mod.foo);");
        files.put(Paths.get("/a/b/esm-module.js").toAbsolutePath(), "export const foo = 'foo';");
        String root = System.getProperty("user.home");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context context = Context.newBuilder("js").fileSystem(new TestFileSystem(files, root)).allowAllAccess(true).allowExperimentalOptions(true).out(out).err(out).option("js.commonjs-require",
                        "true").option("js.commonjs-require-cwd", root).build();
        context.eval("js", "require('/a/b/cjs-module.js').fooPromise.then(console.log);");
        out.flush();
        Assert.assertEquals("foo\n", out.toString());
    }

    private static class TestFileSystem implements FileSystem {

        private final String root;
        private final Map<Path, String> files;

        TestFileSystem(Map<Path, String> files, String root) {
            this.root = root;
            this.files = files;
        }

        @Override
        public Path parsePath(String path) {
            return Paths.get(path);
        }

        @Override
        public Path toAbsolutePath(Path path) {
            return Paths.get(root).resolve(path).normalize();
        }

        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) {
            return toAbsolutePath(path);
        }

        @Override
        public String getSeparator() {
            return File.separator;
        }

        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) {
            if (path.equals(Paths.get(root))) {
                return;
            }
            if (!files.containsKey(path)) {
                throw new AssertionError();
            }
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
            if (!files.containsKey(path)) {
                throw new AssertionError();
            }
            return Collections.singletonMap("isRegularFile", Boolean.TRUE);
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
            String contents = files.get(path);
            byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
            long size = bytes.length;
            ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(bytes));
            return new SeekableByteChannel() {

                @Override
                public boolean isOpen() {
                    return channel.isOpen();
                }

                @Override
                public void close() throws IOException {
                    channel.close();
                }

                @Override
                public int write(ByteBuffer src) {
                    throw new AssertionError();
                }

                @Override
                public SeekableByteChannel truncate(long sizeParam) {
                    throw new AssertionError();
                }

                @Override
                public long size() {
                    return size;
                }

                @Override
                public int read(ByteBuffer dst) throws IOException {
                    return channel.read(dst);
                }

                @Override
                public SeekableByteChannel position(long newPosition) {
                    throw new AssertionError();
                }

                @Override
                public long position() {
                    throw new AssertionError();
                }
            };
        }

        @Override
        public Path parsePath(URI uri) {
            return Paths.get(uri);
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) {
            throw new AssertionError();
        }

        @Override
        public void delete(Path path) throws IOException {
            throw new AssertionError();
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) {
            throw new AssertionError();
        }
    }
}
