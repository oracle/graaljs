/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class ImportWithCustomFsTest {

    @Test
    public void testHttp() throws IOException {
        final String moduleBody = "export const foo = 41;";
        final String expectedSpecifier = "https://unpkg.com/@esm/ms";
        final String testSrc = "import {foo} from 'https://unpkg.com/@esm/ms'; foo;";
        TestFS fs = new TestFS(expectedSpecifier, moduleBody);
        Value v = assertFsLoads(fs, testSrc);
        Assert.assertEquals(41, v.asInt());
        Assert.assertTrue(fs.uriSpecifiers.contains(expectedSpecifier));
        Assert.assertFalse(fs.stringSpecifiers.contains(expectedSpecifier));
    }

    @Test
    public void testFile() throws IOException {
        final String moduleBody = "export const foo = 42;";
        final String expectedSpecifier = "file://path-to-something";
        final String testSrc = "import {foo} from 'file://path-to-something'; foo;";
        TestFS fs = new TestFS(expectedSpecifier, moduleBody);
        Value v = assertFsLoads(fs, testSrc);
        Assert.assertEquals(42, v.asInt());
        Assert.assertTrue(fs.uriSpecifiers.contains(expectedSpecifier));
        Assert.assertFalse(fs.stringSpecifiers.contains(expectedSpecifier));
    }

    @Test
    public void testPath() throws IOException {
        final String expectedSpecifier = "/path";
        final String moduleBody = "export const foo = 43;";
        final String testSrc = "import {foo} from '/path'; foo;";
        TestFS fs = new TestFS(expectedSpecifier, moduleBody);
        Value v = assertFsLoads(fs, testSrc);
        Assert.assertEquals(43, v.asInt());
        Assert.assertTrue(fs.stringSpecifiers.contains(expectedSpecifier));
        Assert.assertFalse(fs.uriSpecifiers.contains(expectedSpecifier));
    }

    @Test
    public void testBareModuleSpecifier() throws IOException {
        final String expectedSpecifier = "foobar";
        final String moduleBody = "export const foo = 43;";
        final String testSrc = "import {foo} from 'foobar'; foo;";
        TestFS fs = new TestFS(expectedSpecifier, moduleBody);
        Value v = assertFsLoads(fs, testSrc);
        Assert.assertEquals(43, v.asInt());
        Assert.assertTrue(fs.stringSpecifiers.contains(expectedSpecifier));
        Assert.assertFalse(fs.uriSpecifiers.contains(expectedSpecifier));
    }

    @Test
    public void testWithUriSpecifierAndReferrer() throws IOException {
        final String moduleBody = "export const foo = 41;";
        final String expectedSpecifier = "https://unpkg.com/@esm/ms";
        final String testSrc = "import {foo} from 'https://unpkg.com/@esm/ms'; foo;";
        TestFS fs = new TestFS(expectedSpecifier, moduleBody);
        Path sourceFile = Files.createTempFile("tmp-test", ".mjs");
        Files.write(sourceFile, Collections.singletonList(testSrc));
        Value v = assertFsLoads(fs, sourceFile.toFile());
        Assert.assertEquals(41, v.asInt());
        Assert.assertTrue(fs.uriSpecifiers.contains(expectedSpecifier));
        Assert.assertFalse(fs.stringSpecifiers.contains(expectedSpecifier));
    }

    @Test
    public void testBareModuleCommonJsEmulation() throws IOException {
        final String expectedSpecifier = "foobar";
        final String moduleBody = "export const foo = 43;";
        final String testSrc = "import {foo} from 'foobar'; foo;";
        CommonJsTracingTestFs fs = new CommonJsTracingTestFs(expectedSpecifier, moduleBody);

        final Map<String, String> options = new HashMap<>();
        options.put("js.commonjs-require", "true");
        options.put("js.commonjs-require-cwd", "/some/user/folder");

        Context cx = JSTest.newContextBuilder().allowPolyglotAccess(PolyglotAccess.ALL).allowIO(true).fileSystem(fs).allowExperimentalOptions(true).options(options).build();
        Value v = cx.eval(Source.newBuilder(ID, testSrc, "test.mjs").build());
        Assert.assertEquals(43, v.asInt());
        Assert.assertTrue(fs.stringSpecifiers.contains(expectedSpecifier));
        Assert.assertFalse(fs.uriSpecifiers.contains(expectedSpecifier));
        Assert.assertTrue(fs.paths.contains("foobar"));
    }

    @Test
    public void testMultiContextSingleSourceCachedImport() throws IOException {
        try (Engine engine = Engine.newBuilder().allowExperimentalOptions(true).build()) {
            String importedSourceText = "export default function() { return 42; }";
            TestFS fs = new TestFS("imported.mjs", importedSourceText);
            Source src = Source.newBuilder("js", "import f from 'imported.mjs'; f();", "main.mjs").mimeType("application/javascript+module").cached(true).build();

            final int numIters = 3;
            int[] sourceIdentityHashes = new int[numIters];
            int[] parsedIdentityHashes = new int[numIters];
            for (int i = 0; i < numIters; i++) {
                // Clear stale entries from the cache
                System.gc();

                try (Context cx = JSTest.newContextBuilder().engine(engine).allowIO(true).fileSystem(fs).build()) {
                    cx.eval(src);

                    cx.enter();
                    // Attempt to get the same Source as the one imported by the main module.
                    TruffleLanguage.Env currentEnv = JavaScriptLanguage.getCurrentEnv();
                    TruffleFile importedFile = currentEnv.getPublicTruffleFile("imported.mjs");
                    com.oracle.truffle.api.source.Source truffleSource = com.oracle.truffle.api.source.Source.newBuilder("js", importedFile).content(importedSourceText).mimeType(
                                    "application/javascript+module").cached(true).build();
                    sourceIdentityHashes[i] = System.identityHashCode(truffleSource);
                    // Should yield the cached parsed call target!
                    CallTarget ct = currentEnv.parsePublic(truffleSource);
                    parsedIdentityHashes[i] = System.identityHashCode(ct);
                    cx.leave();
                }

                if (i > 0) {
                    if (sourceIdentityHashes[i] != sourceIdentityHashes[i - 1]) {
                        Assert.fail("Source identity is not the same as in the previous context. Source is either not cached or not equal to the cached one.");
                    }
                    if (parsedIdentityHashes[i] != parsedIdentityHashes[i - 1]) {
                        Assert.fail("Parsed call target should have the same identity in all contexts if properly cached.");
                    }
                }
            }
        }
    }

    private static Value assertFsLoads(TestFS fs, File file) throws IOException {
        Context cx = JSTest.newContextBuilder().allowIO(true).fileSystem(fs).build();
        return cx.eval(Source.newBuilder(ID, file).build());
    }

    private static Value assertFsLoads(TestFS fs, String testSrc) throws IOException {
        Context cx = JSTest.newContextBuilder().allowIO(true).fileSystem(fs).build();
        return cx.eval(Source.newBuilder(ID, testSrc, "test.mjs").build());
    }

    private static class CommonJsTracingTestFs extends TestFS {

        private final List<String> paths;

        CommonJsTracingTestFs(String expectedPath, String moduleBody) {
            super(expectedPath, moduleBody);
            this.paths = new LinkedList<>();
        }

        @Override
        public Path parsePath(URI uri) {
            paths.add(uri.toString());
            return super.parsePath(uri);
        }

        @Override
        public String getSeparator() {
            return "/";
        }

        @Override
        public Path parsePath(String path) {
            paths.add(path);
            return super.parsePath(path);
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
            Map<String, Object> attr = new HashMap<>();
            // for testing purposes, we consider all files non-regular. In this way, we force the
            // module loader to try all possible file names before throwing module not found
            attr.put("isRegularFile", false);
            return attr;
        }
    }

    protected static class TestFS implements FileSystem {

        private final Path dummyPath;
        private final String moduleBody;
        private final String expectedPath;

        protected final Set<String> uriSpecifiers;
        protected final Set<String> stringSpecifiers;

        TestFS(String expectedPath, String moduleBody) {
            this.expectedPath = expectedPath;
            this.moduleBody = moduleBody;
            this.dummyPath = Paths.get("/", expectedPath);
            this.uriSpecifiers = new HashSet<>();
            this.stringSpecifiers = new HashSet<>();
        }

        @Override
        public Path parsePath(URI uri) {
            uriSpecifiers.add(uri.toString());
            if (expectedPath.equals(uri.toString())) {
                return dummyPath;
            } else {
                return Paths.get(uri);
            }
        }

        @Override
        public Path parsePath(String path) {
            stringSpecifiers.add(path);
            if (expectedPath.equals(path)) {
                return dummyPath;
            } else {
                return Paths.get("/", path);
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
        public void delete(Path path) {
            throw new AssertionError();
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
            if (dummyPath.equals(path)) {
                return new ReadOnlySeekableByteArrayChannel(moduleBody.getBytes(StandardCharsets.UTF_8));
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
