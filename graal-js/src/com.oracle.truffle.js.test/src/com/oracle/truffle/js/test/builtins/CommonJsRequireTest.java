/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;

public class CommonJsRequireTest {

    private static class NodeModulesFolder {
        public static NodeModulesFolder create(Path folder, String name) throws IOException {
            return new NodeModulesFolder(folder, name);
        }

        private final String moduleFolderPath;

        private NodeModulesFolder(Path folder, String name) throws IOException {
            String tmpFolder = folder.toAbsolutePath().toString();
            String nodeModulesPath = tmpFolder + File.separator + "node_modules";
            this.moduleFolderPath = nodeModulesPath + File.separator + name;
            try {
                Files.createDirectory(Paths.get(nodeModulesPath));
            } catch (FileAlreadyExistsException e) {
                // That's OK
            }
            Files.createDirectory(Paths.get(moduleFolderPath));
        }
    }

    private static class TestFile {
        private final String absolutePath;

        public static TestFile create(Path folder, String fileName, String src) throws IOException {
            String path = folder.toAbsolutePath().toString();
            return new TestFile(path, fileName, src);
        }

        public static TestFile create(NodeModulesFolder folder, String fileName, String src) throws IOException {
            String path = folder.moduleFolderPath;
            return new TestFile(path, fileName, src);
        }

        private TestFile(String path, String fileName, String src) throws IOException {
            File tmpFile = new File(path + File.separator + fileName);
            tmpFile.deleteOnExit();
            FileWriter fileWriter = new FileWriter(tmpFile);
            fileWriter.write(src);
            fileWriter.flush();
            this.absolutePath = tmpFile.getAbsolutePath();
        }

        String getAbsolutePath() {
            return absolutePath;
        }
    }

    private static Context testContext(Path tempFolder) {
        return testContext(tempFolder, System.out, System.err);
    }

    private static Context testContext(Path tempFolder, OutputStream out, OutputStream err) {
        return Context.newBuilder(ID).allowPolyglotAccess(PolyglotAccess.ALL)
                .allowExperimentalOptions(true)
                .option("js.cjs-require", "true")
                .option("js.cjs-require-cwd", tempFolder.toAbsolutePath().toString())
                .out(out)
                .err(err)
                .allowIO(true).build();
    }

    private static Path getTempFolder() throws IOException {
        return Files.createTempDirectory("commonjs_testing");
    }

    private void testBasicPackageJsonRequire(String moduleName, String packageJson) throws IOException {
        Path f = getTempFolder();
        try (Context cx = testContext(f)) {
            NodeModulesFolder nm = NodeModulesFolder.create(f, "foo");
            if (!"".equals(packageJson)) {
                TestFile.create(nm, "package.json", packageJson);
            }
            TestFile.create(nm,"index.js", "module.exports.foo = 42;");
            Value js = cx.eval(ID, "require(" + moduleName + ").foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    private void testBasicPackageJsonRequire(String moduleName) throws IOException {
        testBasicPackageJsonRequire(moduleName, "{\"main\":\"index.js\"}");
    }

    private void testBasicRequire(String moduleName) throws IOException {
        Path f = getTempFolder();
        try (Context cx = testContext(f)) {
            TestFile.create(f,"module.js", "module.exports.foo = 42;");
            Value js = cx.eval(ID, "require('./module').foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    private static void assertThrows(String src, Class<? extends Throwable> expected, String expectedMessage) {
        try {
            Path f = getTempFolder();
            try (Context cx = testContext(f)) {
                cx.eval(ID, src);
            }
            assert false;
        } catch (Throwable t) {
            if (!t.getClass().isAssignableFrom(expected)) {
                throw new AssertionError("Unexpected exception " + t);
            }
            assertEquals(expectedMessage, t.getMessage());
        }
    }

    @Test
    public void absoluteFilename() throws IOException {
        Path f = getTempFolder();
        try (Context cx = testContext(f)) {
            TestFile m = TestFile.create(f,"module.js", "module.exports.foo = 42;");
            Value js = cx.eval(ID, "require('" + m.getAbsolutePath() + "').foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void relativeFilename() throws IOException {
        testBasicRequire("./module.js");
    }

    @Test
    public void relativeNoExtFilename() throws IOException {
        testBasicRequire("./module");
    }

    @Test
    public void nodeModulesFolderWithPackageJson() throws IOException {
        testBasicPackageJsonRequire("'foo'");
    }

    @Test
    public void nodeModulesFolderWithPackageJson2() throws IOException {
        testBasicPackageJsonRequire("'./foo'");
    }

    @Test
    public void nodeModulesFolderWithPackageJson3() throws IOException {
        testBasicPackageJsonRequire("'././foo'");
    }

    @Test
    public void nodeModulesFolderWithPackageJsonNoMain() throws IOException {
        testBasicPackageJsonRequire("'foo'", "{\"not_a_valid_main\":\"index.js\"}");
    }

    @Test
    public void nodeModulesFolderWithPackageJsonNoMain2() throws IOException {
        testBasicPackageJsonRequire("'./foo'", "{\"not_a_valid_main\":\"index.js\"}");
    }

    @Test
    public void testMissingPackageJson() throws IOException {
        testBasicPackageJsonRequire("'foo'", "");
    }

    @Test
    public void testMissingPackageJson2() throws IOException {
        testBasicPackageJsonRequire("'foo'", "");
    }

    @Test
    public void nestedRequire() throws IOException {
        Path f = getTempFolder();
        try (Context cx = testContext(f)) {
            TestFile.create(f,"a.js", "module.exports.foo = 42;");
            TestFile.create(f,"b.js", "const a = require('./a.js');" +
                    "exports.foo = a.foo;");
            Value js = cx.eval(ID, "require('./b.js').foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void cyclicRequire() throws IOException {
        Path f = getTempFolder();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        try (Context cx = testContext(f, out, err)) {
            TestFile.create(f,"a.js", "console.log('a starting');" +
                    "exports.done = false;" +
                    "const b = require('./b.js');" +
                    "console.log('in a, b.done = ' + b.done);" +
                    "exports.done = true;" +
                    "console.log('a done');");
            TestFile.create(f,"b.js", "console.log('b starting');" +
                    "exports.done = false;" +
                    "const a = require('./a.js');" +
                    "console.log('in b, a.done = ' + a.done);" +
                    "exports.done = true;" +
                    "console.log('b done');");
            Value js = cx.eval(ID, "console.log('main starting');" +
                    "const a = require('./a.js');" +
                    "const b = require('./b.js');" +
                    "console.log('in main, a.done = ' + a.done + ', b.done = ' + b.done);" +
                    "42;");
            out.flush();
            err.flush();
            String outPrint = new String(out.toByteArray());
            String errPrint = new String(err.toByteArray());

            Assert.assertEquals("main starting\n" +
                    "a starting\n" +
                    "b starting\n" +
                    "in b, a.done = false\n" +
                    "b done\n" +
                    "in a, b.done = true\n" +
                    "a done\n" +
                    "in main, a.done = true, b.done = true\n", outPrint);
            Assert.assertEquals("", errPrint);
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void unknownModule() {
        assertThrows("require('unknown')", PolyglotException.class, "TypeError: Cannot load Npm module: 'unknown'");
    }

    @Test
    public void unknownFile() {
        assertThrows("require('./unknown')", PolyglotException.class, "TypeError: Cannot load Npm module: './unknown'");
    }

    @Test
    public void unknownFileWithExt() {
        assertThrows("require('./unknown.js')", PolyglotException.class, "TypeError: Cannot load Npm module: './unknown.js'");
    }

    @Test
    public void unknownAbsolute() {
        assertThrows("require('/path/to/unknown.js')", PolyglotException.class, "TypeError: Cannot load Npm module: '/path/to/unknown.js'");
    }

    @Test
    public void testLoadJson() throws IOException {
        Path f = getTempFolder();
        try (Context cx = testContext(f)) {
            TestFile.create(f, "foo.json", "{\"foo\":42}");
            Value js = cx.eval(ID, "require('./foo.json').foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void testLoadBrokenJson() throws IOException {
        Path f = getTempFolder();
        try (Context cx = testContext(f)) {
            TestFile.create(f, "foo.json", "{not_a_valid:##json}");
            Value js = cx.eval(ID, "require('./foo.json').foo;");
            assert false;
        } catch (Throwable t) {
            if (!t.getClass().isAssignableFrom(PolyglotException.class)) {
                throw new AssertionError("Unexpected exception " + t);
            }
            assertEquals(t.getMessage(), "SyntaxError: Invalid JSON: <json>:1:1 Expected , or } but found n\n" +
                    "{not_a_valid:##json}\n" +
                    " ^");
        }
    }

}
