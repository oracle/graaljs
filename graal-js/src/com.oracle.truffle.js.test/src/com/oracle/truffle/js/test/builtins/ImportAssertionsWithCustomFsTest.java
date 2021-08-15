/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportAssertionsWithCustomFsTest {

    private static void executeTest(TestTuple source) throws IOException {
        executeTest(source, "./test.js");
    }

    private static void executeTest(TestTuple source, String importName) throws IOException {
        TestFileSystem fs = new TestFileSystem();
        fs.add(importName, source.fileContent);
        if (source.additionalModuleName != null) {
            fs.add(source.additionalModuleName, source.additionalModuleBody);
        }
        if (source.isAsync) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (Context context = JSTest.newContextBuilder().allowIO(true).fileSystem(fs).out(out).option(JSContextOptions.CONSOLE_NAME, "true").option(
                            JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false").option(JSContextOptions.IMPORT_ASSERTIONS_NAME, "true").option(JSContextOptions.JSON_MODULES_NAME,
                                            "true").build()) {
                Value asyncFn = context.eval(JavaScriptLanguage.ID, source.statement);
                asyncFn.executeVoid();
            }
            Assert.assertEquals(source.expectedValue + "\n", out.toString());
        } else {
            try (Context context = JSTest.newContextBuilder().allowIO(true).fileSystem(fs).option(JSContextOptions.IMPORT_ASSERTIONS_NAME, "true").option(JSContextOptions.JSON_MODULES_NAME,
                            "true").build()) {
                Value v = context.eval(Source.newBuilder(JavaScriptLanguage.ID, source.statement, "exec.mjs").build());
                Assert.assertEquals(source.expectedValue, v.asString());
            }
        }
    }

    private static void executeStatements(String assertVal) throws IOException {
        for (TestTuple statement : getTestTuples(assertVal)) {
            executeTest(statement);
        }
    }

    private static TestTuple[] getTestTuples(String assertVal) {
        return getTestTuples(assertVal, "./test.js");
    }

    private static TestTuple[] getTestTuples(String assertVal, String importName) {
        TestTuple[] testTuples = new TestTuple[5];
        testTuples[0] = new TestTuple("import { val } from '" + importName + "' assert " + assertVal + "; val;", "export const val = 'value';", "value");
        testTuples[1] = new TestTuple("import json from '" + importName + "' assert " + assertVal + "; json.val;", "let json = { val: 'value' }; export { json as default };", "value");
        testTuples[2] = new TestTuple("import { val } from '" + importName + "'; val;", "export { val } from './test2.js' assert " + assertVal + ";", "value", "./test2.js",
                        "export const val = 'value';",
                        false);
        testTuples[3] = new TestTuple("(async function () { let {default:json} = await import('" + importName + "', { assert: " + assertVal + " } ); console.log(json.val); });",
                        "let json = { val: 'value' }; export { json as default };", "value", true);
        testTuples[4] = new TestTuple("(async function () { let {default:json} = await import('" + importName + "', { assert: " + assertVal + " }, ); console.log(json.val);});",
                        "let json = { val: 'value' }; export { json as default };", "value", true);
        return testTuples;
    }

    @Test
    public void testImportCallWithDanglingComma() throws IOException {
        executeTest(new TestTuple("(async function () { let {default:json} = await import('./test.js',); console.log(json.val); });", "let json = { val: 'value' }; export { json as default };",
                        "value", true));
    }

    @Test
    public void testAssertEmpty() throws IOException {
        executeStatements("{}");
    }

    @Test
    public void testAssertTypeJsonDanglingComma() throws IOException {
        executeStatements("{ test: 'test', }");
    }

    @Test
    public void testAssertStringJson() throws IOException {
        executeStatements("{ 'test': 'test' }");
    }

    @Test
    public void testAssertStringJsonDanglingComma() throws IOException {
        executeStatements("{ 'test': 'test', }");
    }

    @Test
    public void testAssertTypeManyAttributes() throws IOException {
        executeStatements("{ test: 'test', atr1: 'atr1', atr2: 'atr2' }");
    }

    @Test
    public void testAssertTypeManyAttributesDanglingComma() throws IOException {
        executeStatements("{ test: 'test', atr1: 'atr1', atr2: 'atr2', }");
    }

    @Test
    public void testAssertStringManyAttributes() throws IOException {
        executeStatements("{ 'test': 'test', 'atr1': 'atr1', 'atr2': 'atr2' }");
    }

    @Test
    public void testAssertStringManyAttributesDanglingComma() throws IOException {
        executeStatements("{ 'test': 'test', 'atr1': 'atr1', 'atr2': 'atr2', }");
    }

    @Test
    public void testAssertMixedManyAttributes() throws IOException {
        executeStatements("{ test: 'test', 'atr1': 'atr1', atr2: 'atr2' }");
    }

    @Test
    public void testAssertMixedManyAttributesDanglingComma() throws IOException {
        executeStatements("{ test: 'test', 'atr1': 'atr1', atr2: 'atr2', }");
    }

    @Test
    public void testNoLineTerminator() throws IOException {
        TestTuple t = new TestTuple("var access = ''; Object.defineProperty(globalThis, 'assert', {get:function() { access = 'access'; }}); import { val } from './test.js' " + System.lineSeparator() +
                        "assert" + System.lineSeparator() + " {}; access + ' ' + val;", "export const val = 'value';", "access value");
        TestTuple t2 = new TestTuple("var access = ''; Object.defineProperty(globalThis, 'assert', {get:function() { access = 'access'; }}); import json from './test.js' " + System.lineSeparator() +
                        "assert " + System.lineSeparator() + " {}; access + ' ' + json.val;", "let json = { val: 'value' }; export { json as default };", "access value");
        TestTuple t3 = new TestTuple("import { val } from './test.js'; val;",
                        "var access = ''; Object.defineProperty(globalThis, 'assert', {get:function() { access = 'access'; }}); export { val } from './test2.js' " + System.lineSeparator() +
                                        " assert " + System.lineSeparator() + " {};",
                        "value", "./test2.js", "export const val = 'value';", false);
        executeTest(t);
        executeTest(t2);
        executeTest(t3);
    }

    @Test
    public void testJSONImport() throws IOException {
        TestTuple[] tests = getTestTuples("{ type: 'json' }", "./test.json");
        tests[1].fileContent = "{ \"val\": \"value\"}";
        executeTest(tests[1], "./test.json");
        tests[3].fileContent = "{ \"val\": \"value\"}";
        executeTest(tests[3], "./test.json");
    }

    private static class TestTuple {
        public final String statement;
        public String fileContent;
        public final String expectedValue;
        public final String additionalModuleName;
        public final String additionalModuleBody;
        public final boolean isAsync;

        TestTuple(String statement, String fileContent, String expectedValue) {
            this(statement, fileContent, expectedValue, false);
        }

        TestTuple(String statement, String fileContent, String expectedValue, boolean isAsync) {
            this(statement, fileContent, expectedValue, null, null, isAsync);
        }

        TestTuple(String statement, String fileContent, String expectedValue, String additionalModuleName, String additionalModuleBody, boolean isAsync) {
            this.statement = statement;
            this.fileContent = fileContent;
            this.expectedValue = expectedValue;
            this.additionalModuleName = additionalModuleName;
            this.additionalModuleBody = additionalModuleBody;
            this.isAsync = isAsync;
        }
    }

    private static class TestFileSystem implements FileSystem {
        private final HashMap<Path, String> modules = new HashMap<>();
        private final HashMap<String, Path> names = new HashMap<>();
        protected final Set<String> uriSpecifiers = new HashSet<>();
        protected final Set<String> stringSpecifiers = new HashSet<>();

        public void add(String name, String fileContent) {
            Path p = Paths.get(name).normalize();
            names.put(name, p);
            modules.put(p, fileContent);
        }

        @Override
        public Path parsePath(URI uri) {
            uriSpecifiers.add(uri.toString());
            if (names.containsKey(uri.toString())) {
                return names.get(uri.toString());
            } else {
                return Paths.get(uri);
            }
        }

        @Override
        public Path parsePath(String path) {
            stringSpecifiers.add(path);
            if (names.containsKey(path)) {
                return names.get(path);
            } else {
                return Paths.get(path);
            }
        }

        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) {
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
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
            if (modules.containsKey(path)) {
                return new ReadOnlySeekableByteArrayChannel(modules.get(path).getBytes(StandardCharsets.UTF_8));
            } else {
                throw new AssertionError();
            }
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
            throw new AssertionError();
        }

        @Override
        public Path toAbsolutePath(Path path) {
            return path.toAbsolutePath();
        }

        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) {
            return path;
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
            throw new AssertionError();
        }
    }
}
