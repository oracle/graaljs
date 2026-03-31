/*
 * Copyright (c) 2021, 2026, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_MIME_TYPE;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_CWD_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.oracle.truffle.js.test.interop.AsyncInteropTest.Thenable;

public class CommonJSWithCustomFsTest {

    @Test
    public void testDeniedSync() {
        String src = """
                        (function() {
                          try {
                            require('does.not.exist');
                          } catch(e) {
                            return 'got exception: ' + e;
                          }
                        })""";
        DenyAllFs fs = new DenyAllFs();
        Context ctx = createTestContext(fs, "./");
        fs.denyAll();
        Value doesCatch = ctx.eval(Source.create("js", src));
        Value execute = doesCatch.execute();
        assertEquals("got exception: TypeError: Cannot load module: 'does.not.exist': Not allowed by this FileSystem.", execute.asString());
    }

    @Test
    public void testDeniedAsync() {
        String js = """
                        async function asyncFun(thenable) {
                          await thenable();
                          try {
                            require('does.not.exist');
                          } catch (e) {
                            return 'got exception: ' + e;
                          }
                        }""";
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

    @Test
    public void testNestedBareSpecifier() throws IOException {
        Path testPath = CommonJSRequireTest.getTestRootFolder();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context.Builder contextBuilder = Context.newBuilder("js").allowAllAccess(true).out(out).err(out).allowHostAccess(HostAccess.ALL);
        FileSystem fs = FileSystem.newDefaultFileSystem();
        fs.setCurrentWorkingDirectory(testPath);
        contextBuilder.allowIO(IOAccess.newBuilder().fileSystem(fs).build());
        contextBuilder.option(COMMONJS_REQUIRE_NAME, "true");
        contextBuilder.option(COMMONJS_REQUIRE_CWD_NAME, testPath.toAbsolutePath().toString());
        Context context = contextBuilder.build();
        File jsFile = new File(testPath + "/foo/main.mjs");
        context.eval(Source.newBuilder("js", jsFile).build());
        out.flush();
        Assert.assertEquals("42\n", out.toString());
    }

    @Test
    public void testMultiContextCachedCJSModuleSourceWithImport() throws IOException {
        Path importedModuleFile = Paths.get("imported.js").toAbsolutePath().normalize();
        Path importedModuleDir = requireParent(importedModuleFile);
        String importedModuleName = requireFileName(importedModuleFile);
        String commonJSModuleBody = "exports.foo = 42;";
        Source src = Source.newBuilder("js", "import {foo} from './imported.js'; foo;", "main.mjs").mimeType(MODULE_MIME_TYPE).build();
        StringBuilder log = new StringBuilder();

        try (Engine engine = Engine.newBuilder().allowExperimentalOptions(true).build()) {
            SourceCacheFs fs = new SourceCacheFs(importedModuleFile, commonJSModuleBody);
            final int numIters = 3;
            int[] sourceIdentityHashes = new int[numIters];
            for (int i = 0; i < numIters; i++) {
                System.gc();

                try (Context cx = createTestContext(engine, fs, importedModuleDir.toString())) {
                    Value v = cx.eval(src);
                    Assert.assertEquals(42, v.asInt());

                    log.append("sources(").append(i).append("): ");
                    log.append(engine.getCachedSources().stream().map(source -> source.getName() + ":" + getSourceIdentityHash(source)).collect(Collectors.joining(", ", "[", "]")));
                    log.append("\n");

                    Source importedCommonJsSource = findImportedCommonJsSource(engine, importedModuleName);
                    sourceIdentityHashes[i] = getSourceIdentityHash(importedCommonJsSource);
                }

                if (i > 0) {
                    Assert.assertEquals("Source should be stable across contexts: " + importedModuleName + "\n" + log, sourceIdentityHashes[i - 1], sourceIdentityHashes[i]);
                }
            }
        }
    }

    @Test
    public void testMultiContextCachedCJSModuleSourceWithRequire() throws IOException {
        Path importedModuleFile = Paths.get("imported.js").toAbsolutePath().normalize();
        Path importedModuleDir = requireParent(importedModuleFile);
        String importedModuleName = requireFileName(importedModuleFile);
        String commonJSModuleBody = "exports.foo = 42;";
        Source src = Source.newBuilder("js", "const {foo} = require('./imported.js'); foo;", "main.js").build();
        StringBuilder log = new StringBuilder();

        try (Engine engine = Engine.newBuilder().allowExperimentalOptions(true).build()) {
            SourceCacheFs fs = new SourceCacheFs(importedModuleFile, commonJSModuleBody);
            final int numIters = 3;
            int[] sourceIdentityHashes = new int[numIters];
            for (int i = 0; i < numIters; i++) {
                System.gc();

                try (Context cx = createTestContext(engine, fs, importedModuleDir.toString())) {
                    Value v = cx.eval(src);
                    Assert.assertEquals(42, v.asInt());

                    log.append("sources(").append(i).append("): ");
                    log.append(engine.getCachedSources().stream().map(source -> source.getName() + ":" + getSourceIdentityHash(source)).collect(Collectors.joining(", ", "[", "]")));
                    log.append("\n");

                    Source importedCommonJsSource = findImportedCommonJsSource(engine, importedModuleName);
                    sourceIdentityHashes[i] = getSourceIdentityHash(importedCommonJsSource);
                }

                if (i > 0) {
                    Assert.assertEquals("Source should be stable across contexts: " + importedModuleName + "\n" + log, sourceIdentityHashes[i - 1], sourceIdentityHashes[i]);
                }
            }
        }
    }

    @Test
    public void testImportCjsModuleWithEnumerableDefaultProperty() throws IOException {
        Path importedModuleFile = Paths.get("imported.js").toAbsolutePath().normalize();
        Path importedModuleDir = requireParent(importedModuleFile);
        String commonJSModuleBody = "module.exports = {default: 41, foo: 42};";
        Source src = Source.newBuilder("js", """
                        import pkg, {default as pkgViaBinding, foo} from './imported.js';
                        JSON.stringify([foo, pkg.foo, pkg.default, pkgViaBinding.foo, pkgViaBinding.default, pkg === pkgViaBinding]);
                        """, "main.mjs").mimeType(MODULE_MIME_TYPE).build();

        try (Context cx = createTestContext(new SourceCacheFs(importedModuleFile, commonJSModuleBody), importedModuleDir.toString())) {
            Value v = cx.eval(src);
            Assert.assertEquals("[42,42,41,42,41,true]", v.asString());
        }
    }

    @Test
    public void testImportCjsModuleWithDefaultOnlyExport() throws IOException {
        Path importedModuleFile = Paths.get("imported.js").toAbsolutePath().normalize();
        Path importedModuleDir = requireParent(importedModuleFile);
        String commonJSModuleBody = "module.exports = 42;";
        Source src = Source.newBuilder("js", """
                        import value, {default as valueViaBinding} from './imported.js';
                        JSON.stringify([value, valueViaBinding, value === valueViaBinding]);
                        """, "main.mjs").mimeType(MODULE_MIME_TYPE).build();

        try (Context cx = createTestContext(new SourceCacheFs(importedModuleFile, commonJSModuleBody), importedModuleDir.toString())) {
            Value v = cx.eval(src);
            Assert.assertEquals("[42,42,true]", v.asString());
        }
    }

    @Test
    public void testImportCjsModuleWithQuotedSpecifier() throws IOException {
        Path importedModuleFile = Paths.get("quo'ted.js").toAbsolutePath().normalize();
        Path importedModuleDir = requireParent(importedModuleFile);
        String commonJSModuleBody = "module.exports = 42;";
        Source src = Source.newBuilder("js", """
                        import value from "./quo'ted.js"; value;
                        """, "main.mjs").mimeType(MODULE_MIME_TYPE).build();

        try (Context cx = createTestContext(new SourceCacheFs(importedModuleFile, commonJSModuleBody), importedModuleDir.toString())) {
            Value v = cx.eval(src);
            Assert.assertEquals(42, v.asInt());
        }
    }

    @Test
    public void testImportCjsModuleWithNonIdentifierExportNames() throws IOException {
        Path importedModuleFile = Paths.get("imported.js").toAbsolutePath().normalize();
        Path importedModuleDir = requireParent(importedModuleFile);
        String commonJSModuleBody = "module.exports = {'a-b': 1, 'sp ace': 2};";
        Source src = Source.newBuilder("js", """
                        import * as ns from './imported.js';
                        JSON.stringify([ns['a-b'], ns['sp ace'], ns.default['a-b'], ns.default['sp ace']]);
                        """, "main.mjs").mimeType(MODULE_MIME_TYPE).build();

        try (Context cx = createTestContext(new SourceCacheFs(importedModuleFile, commonJSModuleBody), importedModuleDir.toString())) {
            Value v = cx.eval(src);
            Assert.assertEquals("[1,2,1,2]", v.asString());
        }
    }

    private static Source findImportedCommonJsSource(Engine engine, String expectedSourceName) {
        Source importedSource = null;
        for (Source source : engine.getCachedSources()) {
            if (expectedSourceName.equals(source.getName())) {
                Assert.assertNull("Expected a single imported CommonJS source.", importedSource);
                importedSource = source;
            }
        }
        Assert.assertNotNull("Expected to find the imported CommonJS source.", importedSource);
        return importedSource;
    }

    private static int getSourceIdentityHash(Source polyglotSource) {
        try {
            Field receiver = Source.class.getDeclaredField("receiver");
            receiver.setAccessible(true);
            Object truffleSource = receiver.get(polyglotSource);
            return System.identityHashCode(truffleSource);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assume.assumeNoException("Unable to make org.graalvm.polyglot.Source.receiver accessible", e);
            return polyglotSource.hashCode();
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

    private static final class SourceCacheFs implements FileSystem {

        private final Path moduleFile;
        private final Path moduleDir;
        private final String moduleBody;
        private final String modulePathString;
        private final String moduleRawPath;
        private final String moduleDirString;
        private final String moduleDirRawPath;

        private SourceCacheFs(Path moduleFile, String moduleBody) {
            this.moduleFile = moduleFile;
            this.moduleDir = requireParent(moduleFile);
            this.moduleBody = moduleBody;
            this.modulePathString = normalizePath(moduleFile.toString());
            this.moduleRawPath = normalizePath(moduleFile.toUri().getRawPath());
            this.moduleDirString = normalizePath(moduleDir.toString());
            this.moduleDirRawPath = normalizePath(moduleDir.toUri().getRawPath());
        }

        @Override
        public Path parsePath(URI uri) {
            if (matchesModule(uri.getPath())) {
                return moduleFile;
            } else if (matchesModuleDir(uri.getPath())) {
                return moduleDir;
            }
            return Paths.get(uri);
        }

        @Override
        public Path parsePath(String path) {
            if (matchesModule(path)) {
                return moduleFile;
            } else if (matchesModuleDir(path)) {
                return moduleDir;
            }
            return Paths.get(path);
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
        public void copy(Path source, Path target, CopyOption... options) {
            throw new AssertionError();
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) {
            throw new AssertionError();
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
            if (moduleFile.equals(path)) {
                return new ReadOnlySeekableByteArrayChannel(moduleBody.getBytes(StandardCharsets.UTF_8));
            }
            throw new AssertionError();
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
            throw new AssertionError();
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
            if (moduleFile.equals(path)) {
                return Map.of(
                                "isRegularFile", true,
                                "isDirectory", false);
            } else if (moduleDir.equals(path)) {
                return Map.of(
                                "isRegularFile", false,
                                "isDirectory", true);
            } else {
                return Map.of(
                                "isRegularFile", false,
                                "isDirectory", false);
            }
        }

        @Override
        public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
            throw new AssertionError();
        }

        @Override
        public Path toAbsolutePath(Path path) {
            return path.isAbsolute() ? path : moduleDir.resolve(path).normalize();
        }

        @Override
        public void setCurrentWorkingDirectory(Path currentWorkingDirectory) {
        }

        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) {
            return toAbsolutePath(path);
        }

        @Override
        public Path getTempDirectory() {
            return moduleDir;
        }

        @Override
        public void createLink(Path link, Path existing) {
            throw new AssertionError();
        }

        @Override
        public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) {
            throw new AssertionError();
        }

        @Override
        public Path readSymbolicLink(Path link) {
            throw new AssertionError();
        }

        @Override
        public boolean isSameFile(Path path1, Path path2, LinkOption... options) {
            return path1.normalize().equals(path2.normalize());
        }

        private boolean matchesModule(String path) {
            String normalizedPath = normalizePath(path);
            return normalizedPath.equals(modulePathString) || normalizedPath.equals(moduleRawPath);
        }

        private boolean matchesModuleDir(String path) {
            String normalizedPath = normalizePath(path);
            return normalizedPath.equals(moduleDirString) || normalizedPath.equals(moduleDirRawPath);
        }

        private static String normalizePath(String path) {
            return path.replace('\\', '/');
        }
    }

    private static Path requireParent(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            throw new AssertionError("Expected parent path for " + path);
        }
        return parent;
    }

    private static String requireFileName(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            throw new AssertionError("Expected file name for " + path);
        }
        return fileName.toString();
    }

    private static Context createTestContext(FileSystem fs, String cwd) {
        return createTestContext(null, fs, cwd);
    }

    private static Context createTestContext(Engine engine, FileSystem fs, String cwd) {
        final Map<String, String> options = new HashMap<>();
        options.put(COMMONJS_REQUIRE_NAME, "true");
        options.put(COMMONJS_REQUIRE_CWD_NAME, cwd);

        Context.Builder builder = Context.newBuilder("js").allowExperimentalOptions(true).allowHostAccess(HostAccess.ALL).options(options).allowIO(IOAccess.newBuilder().fileSystem(fs).build());
        if (engine != null) {
            builder.engine(engine);
        }
        return builder.build();
    }
}
