/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_CWD_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_NAME;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.test.builtins.ReadOnlySeekableByteArrayChannel;

public class GR36103 {

    private static void test(String code, String packageJson) {
        String moduleBody = "export class Foo { bar() { return 42; } }";
        Context context = createDefaultContext(Context.newBuilder("js").allowAllAccess(true), moduleBody, packageJson);
        Source source = Source.newBuilder("js", code, "test.mjs").buildLiteral();
        var val = context.eval(source);
        Assert.assertTrue(val.isNumber());
        Assert.assertTrue(val.fitsInInt());
        Assert.assertEquals(42, val.asInt());
    }

    @Test
    public void reproducer() {
        test("import {Foo} from 'js/foo.mjs'; (new Foo).bar();", null);
    }

    @Test
    public void reproducerBare() {
        String packageJson = "{\"type\":\"module\", \"main\":\"foo.mjs\"}";
        test("import {Foo} from 'foo'; (new Foo).bar();", packageJson);
    }

    @Test
    public void reproducerAt() {
        test("import {Foo} from '@js/nested/foo.mjs'; (new Foo).bar();", null);
    }

    static Context createDefaultContext(Context.Builder builder, String moduleBody, String packageJson) {
        Map<String, String> options = new HashMap<>();
        options.put(COMMONJS_REQUIRE_NAME, "true");
        options.put(COMMONJS_REQUIRE_CWD_NAME, "node/npm");
        builder.options(options).allowIO(IOAccess.newBuilder().fileSystem(new TestFileSystem(moduleBody, packageJson)).build());
        return builder.build();
    }

    private static final class TestFileSystem implements FileSystem {

        private final String moduleBody;
        private final String packageJson;

        TestFileSystem(String moduleBody, String packageJson) {
            this.moduleBody = moduleBody;
            this.packageJson = packageJson;
        }

        @Override
        public Path parsePath(URI uri) {
            return Path.of(uri);
        }

        @Override
        public Path parsePath(String path) {
            return Path.of(path);
        }

        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        }

        @Override
        public void delete(Path path) throws IOException {
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
            if (path.endsWith("package.json") && packageJson != null) {
                return new ReadOnlySeekableByteArrayChannel(packageJson.getBytes());
            }
            return new ReadOnlySeekableByteArrayChannel(moduleBody.getBytes());
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
            return null;
        }

        @Override
        public Path toAbsolutePath(Path path) {
            return path;
        }

        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
            return path;
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
            Map<String, Object> map = new HashMap<>();
            // In this test, we consider a directory every path that does not contain '.'
            if (path.toString().contains(".")) {
                map.put("isRegularFile", true);
                map.put("isDirectory", false);
            } else {
                map.put("isRegularFile", false);
                map.put("isDirectory", true);
            }
            // `package.json` exists only if used by the test
            if (path.toString().endsWith("package.json")) {
                map.put("isRegularFile", packageJson != null);
                map.put("isDirectory", false);
            }
            return map;
        }
    }
}
