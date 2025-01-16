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
package com.oracle.truffle.js.test.interop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.Assume;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.builtins.ReadOnlySeekableByteArrayChannel;

public class MLEBuiltinTest {

    public static class TestEsmLookup implements ProxyExecutable {

        private final Map<String, String> testMapping;

        TestEsmLookup(Map<String, String> mapping) {
            this.testMapping = mapping;
        }

        @Override
        public Object execute(Value... args) {
            String referrer = args[0].asString();
            String symbol = args[1].asString();

            for (String tuple : testMapping.keySet()) {
                String[] expected = tuple.split(":");
                String expectedSymbol = expected.length == 1 ? tuple : expected[0];
                String expectedReferrer = expected.length == 1 ? null : expected[1];
                if (expectedSymbol.equals(symbol)) {
                    String mappedUrl = testMapping.get(tuple);
                    if (expectedReferrer != null && expectedReferrer.equals(referrer)) {
                        return mappedUrl;
                    } else if (expectedReferrer == null && referrer == null) {
                        return mappedUrl;
                    }
                }
            }
            // No mapping: returning null will cause an exception.
            return null;
        }
    }

    @Test
    public void testNoReferencer() throws IOException {
        Map<String, String> testMappings = new HashMap<>();
        testMappings.put("foo", "/foo/bar");
        TestEsmLookup importCallback = new TestEsmLookup(testMappings);
        Map<String, String> virtualModules = new HashMap<>();
        virtualModules.put("/foo/bar", "export function fun() { return '42'; };");
        Source src = Source.newBuilder("js", "import {fun} from 'foo';" +
                        "fun();",
                        "test.mjs").build();
        try (Context cx = getTestContext(importCallback, virtualModules)) {
            assertEquals("42", cx.eval(src).asString());
        }
    }

    @Test
    public void testWithReferencer() throws IOException {
        Assume.assumeFalse(System.getProperty("os.name").contains("Windows"));
        Map<String, String> testMappings = new HashMap<>();
        testMappings.put("foo:/some/ref.mjs", "/foo/bar");
        testMappings.put("/some/ref.mjs", "/some/ref.mjs");
        TestEsmLookup importCallback = new TestEsmLookup(testMappings);
        Map<String, String> virtualModules = new HashMap<>();
        virtualModules.put("/foo/bar", "export function fun() { return '42'; };");
        virtualModules.put("/some/ref.mjs", "import {fun} from 'foo'; export function fun2() { return fun() + '42'; };");
        Source src = Source.newBuilder("js", "import {fun2} from '/some/ref.mjs';" +
                        "fun2();",
                        "src.mjs").build();
        try (Context cx = getTestContext(importCallback, virtualModules)) {
            assertEquals("4242", cx.eval(src).asString());
        }
    }

    @Test
    public void testUnknownSymbol() throws IOException {
        TestEsmLookup importCallback = new TestEsmLookup(new HashMap<>());
        Map<String, String> virtualModules = new HashMap<>();
        Source src = Source.newBuilder("js", "import {foo} from 'wrong';", "src.mjs").build();
        Context cx = getTestContext(importCallback, virtualModules);
        boolean failed = false;
        try {
            cx.eval(src);
        } catch (PolyglotException e) {
            failed = true;
            assertEquals("Error: Cannot load ES module: wrong", e.getMessage());
        } finally {
            cx.close();
        }
        assertTrue(failed);
        cx.close();
    }

    private static Context getTestContext(TestEsmLookup importCallback, Map<String, String> virtualModules) {
        FileSystem testFs = new TestFileSystem(virtualModules);
        IOAccess ioAccess = IOAccess.newBuilder().fileSystem(testFs).build();
        Context context = JSTest.newContextBuilder().option(JSContextOptions.MLE_MODE_NAME, "true").option("engine.WarnInterpreterOnly", "false").allowIO(ioAccess).allowAllAccess(true).build();
        context.getBindings("js").putMember("lambda", importCallback);
        context.eval("js", "MLE.registerESMLookup(lambda);");
        context.getBindings("js").removeMember("lambda");
        return context;
    }

    private static final class TestFileSystem implements FileSystem {

        private Path expectedPath;
        private String expectedModule;
        private final Map<String, String> virtualModules;

        TestFileSystem(Map<String, String> virtualModules) {
            this.virtualModules = virtualModules;
        }

        @Override
        public Path parsePath(URI uri) {
            throw new AssertionError();
        }

        @Override
        public Path parsePath(String path) {
            if (virtualModules.containsKey(path)) {
                this.expectedPath = Paths.get(path).toAbsolutePath();
                this.expectedModule = virtualModules.get(path);
                return expectedPath;
            } else {
                return Paths.get(path);
            }
        }

        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
            throw new AssertionError();
        }

        @Override
        public void delete(Path path) throws IOException {
            throw new AssertionError();
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
            if (path.equals(expectedPath) && expectedModule != null) {
                return new ReadOnlySeekableByteArrayChannel(expectedModule.getBytes(StandardCharsets.UTF_8));
            } else {
                throw new AssertionError();
            }
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
            throw new AssertionError();
        }

        @Override
        public Path toAbsolutePath(Path path) {
            return path.toAbsolutePath();
        }

        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
            if (path.equals(expectedPath)) {
                return expectedPath;
            } else {
                return path;
            }
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
            throw new AssertionError();
        }
    }

}
