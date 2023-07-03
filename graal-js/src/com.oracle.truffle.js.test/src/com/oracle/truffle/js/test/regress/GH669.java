/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.js.test.builtins.ReadOnlySeekableByteArrayChannel;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_CWD_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GH669 {

    private static final String JS = "js";
    private static final String MODULE_PATH = Paths.get("a/b/c").toAbsolutePath().toString();
    private static final String INDEX_PATH = absPath("index.js");
    private static final String ERROR_HELPER_PATH = absPath("error_helper.js");
    private static final Pattern JS_STACK_FRAME_PATTERN = Pattern.compile("^\\s*at ([\\w.]+) \\(([\\w/.-]+):\\d+:\\d+\\)$");

    @Test
    public void testJavaStackTrace() {
        try (var ctx = createContext()) {
            var script = "require('%s').regularFunction();".formatted(INDEX_PATH);
            var frames = getJavaStackTrace(() -> ctx.eval(JS, script));
            assertJavaStackFrame(frames[0], "throw3", ERROR_HELPER_PATH);
            assertJavaStackFrame(frames[1], "throw2", ERROR_HELPER_PATH);
            assertJavaStackFrame(frames[2], "throw1", ERROR_HELPER_PATH);
            assertJavaStackFrame(frames[3], "regularFunction", INDEX_PATH);
        }
    }

    @Test
    public void testJavaStackTraceAsync() {
        var out = new ByteArrayOutputStream();

        try (var ctx = createContext()) {
            var script = "require('%s').asyncFunction();".formatted(INDEX_PATH);
            var promise = ctx.eval(JS, script);
            promise.invokeMember("catch", (Consumer<Object>) e -> {
                var stackTrace = (String) ((Map<?, ?>) e).get("stack");
                out.writeBytes(stackTrace.getBytes());
            });
        }

        var frames = getJsStackTrace(out.toString());
        assertJsStackFrame(frames[0], "throw3", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[1], "throw2", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[2], "Object.throw1", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[3], "Object.asyncFunction", INDEX_PATH);
    }

    @Test
    public void testJsStackTrace() {
        var out = new ByteArrayOutputStream();

        try (var ctx = createContext(out)) {
            var script = """
                                try {
                                    require('%s').regularFunction();
                                } catch(e) {
                                    console.error(e.stack);
                                }
                            """.formatted(INDEX_PATH);
            ctx.eval(JS, script);
        }

        var frames = getJsStackTrace(out.toString());
        assertJsStackFrame(frames[0], "throw3", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[1], "throw2", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[2], "Object.throw1", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[3], "Object.regularFunction", INDEX_PATH);
    }

    @Test
    public void testJsStackTraceAsync() {
        var out = new ByteArrayOutputStream();

        try (var ctx = createContext(out)) {
            var script = """
                                (async () => {
                                    try {
                                        await (require('%s').asyncFunction());
                                    } catch(e) {
                                        console.error(e.stack);
                                    }
                                })();
                            """.formatted(INDEX_PATH);
            ctx.eval(JS, script);
        }

        var frames = getJsStackTrace(out.toString());
        assertJsStackFrame(frames[0], "throw3", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[1], "throw2", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[2], "Object.throw1", ERROR_HELPER_PATH);
        assertJsStackFrame(frames[3], "Object.asyncFunction", INDEX_PATH);
    }

    private static String absPath(String filename) {
        return Path.of(MODULE_PATH, filename).toString();
    }

    private static Context createContext() {
        return createContextBuilder().build();
    }

    private static Context createContext(OutputStream out) {
        return createContextBuilder().out(out).err(out).build();
    }

    private static Context.Builder createContextBuilder() {
        var errorHelperContents = """
                        module.exports.throw1 = function() { throw2(); }
                        function throw2() { throw3(); }
                        function throw3() { throw new Error('boom'); }
                        """;

        var indexContents = """
                        const errorHelper = require('%s');
                        module.exports.regularFunction = function() { errorHelper.throw1(); }
                        module.exports.asyncFunction = async function() { errorHelper.throw1(); }
                        """.formatted(ERROR_HELPER_PATH);

        var files = Map.of(
                        Path.of(ERROR_HELPER_PATH), errorHelperContents,
                        Path.of(INDEX_PATH), indexContents);

        var ioAccess = IOAccess.newBuilder().fileSystem(new TestFileSystem(files, MODULE_PATH)).build();
        return Context.newBuilder(JS).allowIO(ioAccess).allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).option(COMMONJS_REQUIRE_NAME, "true").option(COMMONJS_REQUIRE_CWD_NAME,
                        MODULE_PATH);
    }

    private static StackTraceElement[] getJavaStackTrace(Callable<?> callable) {
        try {
            callable.call();
        } catch (Throwable e) {
            return e.getStackTrace();
        }

        throw new AssertionError("An exception wasn't thrown");
    }

    private static void assertJavaStackFrame(StackTraceElement frame, String methodName, String path) {
        assertEquals("<js>", frame.getClassName());
        assertEquals(methodName, frame.getMethodName());
        assertEquals(path, frame.getFileName());
    }

    private static String[] getJsStackTrace(String stackTrace) {
        return Arrays.stream(stackTrace.split("\n")).skip(1).toArray(String[]::new);
    }

    private static void assertJsStackFrame(String frame, String methodName, String path) {
        var matcher = JS_STACK_FRAME_PATTERN.matcher(frame);
        assertTrue(matcher.find());
        assertEquals(methodName, matcher.group(1));
        assertEquals(path, matcher.group(2));
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
            var attrs = new HashMap<String, Object>();
            attrs.put("isRegularFile", Boolean.TRUE);
            attrs.put("isDirectory", Boolean.FALSE);
            return attrs;
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
            var contents = files.get(path);
            var bytes = contents.getBytes(StandardCharsets.UTF_8);
            return new ReadOnlySeekableByteArrayChannel(bytes);
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
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
            throw new AssertionError();
        }
    }
}
