/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_CWD_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.builtins.ReadOnlySeekableByteArrayChannel;

public class GH1147 {

    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testMappedModuleFormats() throws IOException {
        String esm = "export const answer = 42;";
        String cjs = "exports.answer = 42;";
        MemoryFileSystem fs = newFileSystem(Map.of(
                        "module.mjs", esm,
                        "module.cjs", cjs,
                        "esm/module.js", esm,
                        "esm/package.json", "{\"type\":\"module\"}",
                        "cjs/module.js", cjs,
                        "cjs/package.json", "{\"type\":\"commonjs\"}",
                        "default.js", cjs), Map.of(
                                        "myapp:esm", "module.mjs",
                                        "myapp:cjs", "module.cjs",
                                        "myapp:esm-package", "esm/module.js",
                                        "myapp:cjs-package", "cjs/module.js",
                                        "myapp:default", "default.js"));
        try (Context context = newContext(fs, true)) {
            int index = 0;
            for (String specifier : fs.mappings.keySet()) {
                Value answer = evalModule(context, "import {answer} from '" + specifier + "'; answer;", "main" + index++ + ".mjs");
                assertEquals(specifier, 42, answer.asInt());
                context.eval(ID, "globalThis.dynamicAnswer = 0; import('" + specifier + "').then(m => globalThis.dynamicAnswer = m.answer);");
                assertEquals(specifier, 42, context.getBindings(ID).getMember("dynamicAnswer").asInt());
                assertEquals(specifier, 1, fs.uriCalls.get(specifier).intValue());
                assertFalse(fs.stringPaths.contains(specifier));
            }
        }
    }

    @Test
    public void testESModuleIdentityAndRelativeImports() throws IOException {
        for (boolean commonjs : new boolean[]{false, true}) {
            MemoryFileSystem fs = newFileSystem(Map.of(
                            "modules with spaces/sdk.mjs", """
                                            import {answer} from './dep.mjs';
                                            globalThis.evaluations = (globalThis.evaluations || 0) + 1;
                                            export {answer};
                                            export const url = import.meta.url;
                                            """,
                            "modules with spaces/dep.mjs", "export const answer = 42;"), Map.of(
                                            "myapp:first", "modules with spaces/sdk.mjs",
                                            "myapp:second", "modules with spaces/sdk.mjs"));
            try (Context context = newContext(fs, commonjs)) {
                assertEquals(42, evalModule(context, "import * as m from 'myapp:first'; globalThis.first = m; m.answer;", "first.mjs").asInt());
                assertTrue(evalModule(context, "import * as m from 'myapp:first'; m === globalThis.first;", "repeat.mjs").asBoolean());
                assertEquals(1, context.getBindings(ID).getMember("evaluations").asInt());
                assertEquals(1, fs.uriCalls.get("myapp:first").intValue());
                assertEquals(fs.path("modules with spaces/sdk.mjs").toUri().toString(), context.getBindings(ID).getMember("first").getMember("url").asString());
                assertTrue(evalModule(context, "import * as m from 'myapp:second'; m !== globalThis.first;", "alias.mjs").asBoolean());
                assertEquals(2, context.getBindings(ID).getMember("evaluations").asInt());
            }
        }
    }

    @Test
    public void testCommonJSIdentityAndRelativeRequires() throws IOException {
        MemoryFileSystem fs = newFileSystem(Map.of(
                        "modules with spaces/sdk.cjs", """
                                        globalThis.evaluations = (globalThis.evaluations || 0) + 1;
                                        exports.answer = require('./dep.cjs').answer;
                                        """,
                        "modules with spaces/dep.cjs", "exports.answer = 42;"), Map.of(
                                        "myapp:first", "modules with spaces/sdk.cjs",
                                        "myapp:second", "modules with spaces/sdk.cjs"));
        try (Context context = newContext(fs, true)) {
            assertEquals(42, evalModule(context, "import * as m from 'myapp:first'; globalThis.first = m; m.answer;", "first.mjs").asInt());
            assertTrue(evalModule(context, "import * as m from 'myapp:first'; m === globalThis.first;", "repeat.mjs").asBoolean());
            assertEquals(1, fs.uriCalls.get("myapp:first").intValue());
            assertTrue(evalModule(context, "import * as m from 'myapp:second'; m !== globalThis.first && m.default === globalThis.first.default;", "alias.mjs").asBoolean());
            assertEquals(1, context.getBindings(ID).getMember("evaluations").asInt());
        }
    }

    @Test
    public void testDynamicAliasesFromScript() throws IOException {
        assertDynamicAliasesFromScript("");
    }

    @Test
    public void testDynamicAliasesWithIgnoredImportAttributes() throws IOException {
        assertDynamicAliasesFromScript(", {with: {ignored: 'value'}}");
    }

    private void assertDynamicAliasesFromScript(String options) throws IOException {
        for (String extension : new String[]{"mjs", "cjs"}) {
            String content = extension.equals("mjs") ? "export const answer = 42;" : "exports.answer = 42;";
            MemoryFileSystem fs = newFileSystem(Map.of("module." + extension, content), Map.of(
                            "myapp:first", "module." + extension,
                            "myapp:second", "module." + extension));
            try (Context context = newContext(fs, true)) {
                context.eval(ID, "globalThis.modules = [];");
                for (String specifier : new String[]{"myapp:first", "myapp:second", "myapp:first"}) {
                    context.eval(ID, "import('" + specifier + "'" + options + ").then(m => globalThis.modules.push(m));");
                }
                assertTrue(context.eval(ID, "modules.length === 3 && modules.every(m => m.answer === 42) && modules[0] === modules[2] && modules[0] !== modules[1]").asBoolean());
            }
        }
    }

    @Test
    public void testArchiveMappingsAreRejected() throws IOException {
        Path archive = temporaryFolder.getRoot().toPath().resolve("modules.zip");
        try (var nioFileSystem = FileSystems.newFileSystem(URI.create("jar:" + archive.toUri()), Map.of("create", "true"))) {
            MemoryFileSystem fs = new MemoryFileSystem(nioFileSystem.getPath("/"), Map.of(
                            "module.mjs", "export const answer = 42;"), Map.of("myapp:module", "module.mjs"));
            try (Context context = newContext(fs, true)) {
                Source source = Source.newBuilder(ID, "import 'myapp:module';", "main.mjs").build();
                JSTest.assertThrows(() -> context.eval(source), exception -> {
                    assertTrue(exception.getMessage(), exception.isGuestException());
                    assertFalse(exception.getMessage(), exception.isInternalError());
                    assertEquals("TypeError: Unsupported URL scheme: 'myapp:'", exception.getMessage());
                });
            }
        }
    }

    @Test
    public void testFileURIFormatUsesOriginalURI() throws IOException {
        MemoryFileSystem fs = newFileSystem(Map.of("module.cjs", "export const answer = 42;"), Map.of());
        String uri = fs.path("module.mjs").toUri().toString();
        fs.mappings.put(uri, fs.path("module.cjs"));
        try (Context context = newContext(fs, true)) {
            assertEquals(42, evalModule(context, "import {answer} from '" + uri + "'; answer;", "main.mjs").asInt());
        }
    }

    @Test
    public void testInvalidMappedTargets() throws IOException {
        MemoryFileSystem fs = newFileSystem(Map.of(
                        "directory/index.mjs", "export const answer = 42;",
                        "module.txt", "export const answer = 42;"), Map.of(
                                        "myapp:missing", "missing.mjs",
                                        "myapp:directory", "directory",
                                        "myapp:extension", "module.txt"));
        Map<String, String> errors = Map.of(
                        "myapp:missing", "Module not found:",
                        "myapp:directory", "Unsupported directory import:",
                        "myapp:extension", "Unsupported file extension:");
        try (Context context = newContext(fs, true)) {
            for (var entry : errors.entrySet()) {
                Source source = Source.newBuilder(ID, "import '" + entry.getKey() + "';", "main.mjs").build();
                JSTest.assertThrows(() -> context.eval(source), exception -> {
                    assertTrue(exception.getMessage(), exception.isGuestException());
                    assertFalse(exception.getMessage(), exception.isInternalError());
                    assertTrue(exception.getMessage(), exception.getMessage().contains(entry.getValue()));
                });
            }
        }
    }

    @Test
    public void testDeniedMappedTarget() throws IOException {
        MemoryFileSystem fs = new MemoryFileSystem(temporaryFolder.newFolder().toPath(), Map.of("module.mjs", "export const answer = 42;"), Map.of("myapp:module", "module.mjs")) {
            @Override
            public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
                if (toAbsolutePath(path).equals(path("module.mjs"))) {
                    throw new SecurityException("Access denied to mapped module");
                }
                super.checkAccess(path, modes, linkOptions);
            }
        };
        try (Context context = newContext(fs, true)) {
            Source source = Source.newBuilder(ID, "import 'myapp:module';", "main.mjs").build();
            JSTest.assertThrows(() -> context.eval(source), exception -> {
                assertTrue(exception.getMessage(), exception.isGuestException());
                assertFalse(exception.getMessage(), exception.isInternalError());
                assertTrue(exception.getMessage(), exception.getMessage().contains("Access denied to mapped module"));
            });
        }
    }

    private MemoryFileSystem newFileSystem(Map<String, String> files, Map<String, String> mappings) throws IOException {
        return new MemoryFileSystem(temporaryFolder.newFolder().toPath(), files, mappings);
    }

    private static Context newContext(MemoryFileSystem fs, boolean commonjs) {
        return JSTest.newContextBuilder().allowExperimentalOptions(true).option(COMMONJS_REQUIRE_NAME, Boolean.toString(commonjs)).option(
                        COMMONJS_REQUIRE_CWD_NAME, fs.root.toString()).allowIO(IOAccess.newBuilder().fileSystem(fs).build()).build();
    }

    private static Value evalModule(Context context, String code, String name) throws IOException {
        return context.eval(Source.newBuilder(ID, new File(name)).content(code).build());
    }

    private static class MemoryFileSystem implements FileSystem {
        private final Path root;

        private final Map<Path, String> files = new HashMap<>();
        private final Map<String, Path> mappings = new HashMap<>();
        private final Map<String, Integer> uriCalls = new HashMap<>();
        private final Set<String> stringPaths = new HashSet<>();

        MemoryFileSystem(Path root, Map<String, String> files, Map<String, String> mappings) throws IOException {
            this.root = root.toAbsolutePath().normalize();
            files.forEach((name, content) -> this.files.put(path(name), content));
            mappings.forEach((uri, name) -> this.mappings.put(uri, path(name)));
            // Keep directory URIs correct for ordinary Path objects; file contents stay virtual.
            for (Path file : this.files.keySet()) {
                Path parent = file.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            }
        }

        final Path path(String name) {
            return root.resolve(name).normalize();
        }

        @Override
        public Path parsePath(URI uri) {
            uriCalls.merge(uri.toString(), 1, Integer::sum);
            Path mapped = mappings.get(uri.toString());
            if (mapped != null) {
                return mapped;
            }
            if (root.toUri().getScheme().equals(uri.getScheme())) {
                return root.getFileSystem().provider().getPath(uri);
            }
            if (uri.isOpaque()) {
                throw new UnsupportedOperationException(uri.toString());
            }
            return Path.of(uri);
        }

        @Override
        public Path parsePath(String path) {
            stringPaths.add(path);
            return root.getFileSystem().getPath(path);
        }

        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
            Path absolute = toAbsolutePath(path);
            if (!files.containsKey(absolute) && !isDirectory(absolute)) {
                throw new NoSuchFileException(path.toString());
            }
        }

        private boolean isDirectory(Path path) {
            return path.equals(root) || files.keySet().stream().anyMatch(file -> !file.equals(path) && file.startsWith(path));
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
            checkAccess(path, Set.of(), options);
            Path absolute = toAbsolutePath(path);
            return Map.of("isRegularFile", files.containsKey(absolute), "isDirectory", isDirectory(absolute));
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
            checkAccess(path, Set.of(AccessMode.READ));
            String content = files.get(toAbsolutePath(path));
            if (content == null) {
                throw new NoSuchFileException(path.toString());
            }
            return new ReadOnlySeekableByteArrayChannel(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Path toAbsolutePath(Path path) {
            return (path.isAbsolute() ? path : root.resolve(path)).normalize();
        }

        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
            checkAccess(path, Set.of(), linkOptions);
            return toAbsolutePath(path);
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(Path path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
            throw new UnsupportedOperationException();
        }
    }
}
