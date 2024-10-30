/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_MIME_TYPE;
import static com.oracle.truffle.js.runtime.JSContextOptions.ESM_EVAL_RETURNS_EXPORTS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * Various tests for EcmaScript 6 module loading via {@link Source}.
 */
public class ESModuleTest {

    private static void commonCheck(Value v) {
        assertTrue(v.hasArrayElements());
        assertTrue(v.getArrayElement(0).isNumber());
        assertEquals(121, v.getArrayElement(0).asInt());
        assertTrue(v.getArrayElement(1).isNumber());
        assertEquals(5, v.getArrayElement(1).asInt());
        assertTrue(v.getArrayElement(2).isNumber());
        assertEquals(11, v.getArrayElement(2).asInt());
    }

    private static void copyResourceToFile(String resource, File file) throws IOException {
        copyResourceToFile(resource, file, new String[0][0]);
    }

    /**
     * Helper method to copy a Java resource to a disk file.
     *
     * @param resource relative path to a Java resource. E.g.
     *            <code>"nestedpackage/resource.js"</code> points to a resource
     *            <code>"thisclasspackage/nestedpackage/resource.js"</code>.
     * @param file the file to write the contents of the resource to.
     * @param moduleNameReplacements array of two-element arrays, each two element array
     *            <code>a</code> specifies the replacement of the first occurence of
     *            <code>"'" + a[0] + "'"</code> on each line with <code>"'" + a[1] + "'"</code>. The
     *            replacements are processed in the order specified by the first array.
     */
    private static void copyResourceToFile(String resource, File file, String[][] moduleNameReplacements) throws IOException {
        InputStream inputStream = ESModuleTest.class.getResourceAsStream(resource);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                        BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                for (String[] moduleNameReplacement : moduleNameReplacements) {
                    line = line.replace(moduleNameReplacement[0], moduleNameReplacement[1]);
                }
                bw.write(line);
                bw.write(System.lineSeparator());
            }
        }
    }

    /**
     * @return The suffix of the supplied string starting at the first character after the last
     *         <code>'/'</code> character, or the whole string, if the suplied string contains no
     *         <code>'/'</code> character.
     */
    private static String stripToLastSlash(String resourceName) {
        return resourceName.contains("/") ? resourceName.substring(resourceName.lastIndexOf("/") + 1) : resourceName;
    }

    /**
     * @return The supplied string split into the string before the last <code>'.'</code> character
     *         and the suffix starting at the last <code>'.'</code> character. If the supplied
     *         string contains no <code>'.'</code> character, it is split into the whole string and
     *         an empty string.
     */
    private static String[] baseAndExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        String fileNameBase = lastDot < 0 ? fileName : fileName.substring(0, lastDot);
        String fileNameExtension = fileName.substring(fileNameBase.length());
        return new String[]{fileNameBase, fileNameExtension};
    }

    /**
     * Copies the main file resource and its module resources to files, replacing the references to
     * the modules in the main file as necessary.
     *
     * @param mainFileResource reference to a resource with the main file contents.
     * @param moduleFileResources references to module files resources.
     * @return array of files where all the contents have been copied.
     */
    private static File[] prepareTestFileAndModules(String mainFileResource, String... moduleFileResources) throws IOException {
        List<String> moduleNames = new ArrayList<>();
        List<File> moduleFiles = new ArrayList<>();
        int moduleNamesWithExtensionCount = 0;
        for (String moduleFileResource : moduleFileResources) {
            String moduleName = stripToLastSlash(moduleFileResource);
            String[] moduleNameBaseAndExtension = baseAndExtension(moduleName);
            String moduleNameBase = moduleNameBaseAndExtension[0];
            String moduleNameExtension = moduleNameBaseAndExtension[1];
            if (!moduleNameExtension.isEmpty()) {
                moduleNamesWithExtensionCount++;
            }

            File moduleFile = File.createTempFile(moduleNameBase, moduleNameExtension);
            moduleFile.deleteOnExit();
            copyResourceToFile(moduleFileResource, moduleFile);

            moduleNames.add(moduleName);
            moduleFiles.add(moduleFile);
        }

        String[][] moduleNameReplacements = new String[moduleFiles.size() + moduleNamesWithExtensionCount][];
        int replacementIndex = 0;
        for (int i = 0; i < moduleFiles.size(); i++) {
            String moduleName = moduleNames.get(i);
            String moduleTempFileName = moduleFiles.get(i).getName();

            moduleNameReplacements[replacementIndex++] = new String[]{moduleName, moduleTempFileName};
            if (moduleName.contains(".")) {
                String moduleNameBase = "'" + baseAndExtension(moduleName)[0] + "'";
                String moduleTempFileNameBase = "'" + baseAndExtension(moduleTempFileName)[0] + "'";
                moduleNameReplacements[replacementIndex++] = new String[]{moduleNameBase, moduleTempFileNameBase};
            }
        }

        String mainFileName = stripToLastSlash(mainFileResource);
        String[] mainFileNameBaseAndExtension = baseAndExtension(mainFileName);
        String mainFileNameBase = mainFileNameBaseAndExtension[0];
        String mainFileNameExtension = mainFileNameBaseAndExtension[1];

        File mainFile = File.createTempFile(mainFileNameBase, mainFileNameExtension);
        mainFile.deleteOnExit();
        copyResourceToFile(mainFileResource, mainFile, moduleNameReplacements);

        List<File> allFilesList = new ArrayList<>();
        allFilesList.add(mainFile);
        allFilesList.addAll(moduleFiles);

        return allFilesList.toArray(new File[0]);
    }

    /**
     * Deletes specified files.
     */
    private static void deleteFiles(File[] filesArray) {
        if (filesArray != null) {
            for (File file : filesArray) {
                // noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    /**
     * Test basic function export. To be able to use the module API, MIME type
     * "application/javascript+module" is specified for the main file.
     */
    @Test
    public void testFunctionExport() throws IOException {
        File[] allFilesArray = null;
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {
            allFilesArray = prepareTestFileAndModules(
                            "resources/functionexporttest.js",
                            "resources/functionexportmodule.js");
            Source mainSource = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value v = context.eval(mainSource);
            commonCheck(v);
        } finally {
            deleteFiles(allFilesArray);
        }
    }

    /**
     * Test basic function export. To be able to use the module API, the main file has the ".mjs"
     * extension.
     */
    @Test
    public void testFunctionExportNoMimeType() throws IOException {
        File[] allFilesArray = null;
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {
            allFilesArray = prepareTestFileAndModules(
                            "resources/functionexporttest.js",
                            "resources/functionexportmodule.js");
            String mainFilePath = allFilesArray[0].getAbsolutePath();
            String[] mainFileBaseAndExtension = baseAndExtension(mainFilePath);
            File mainFileWithMjsExtension = new File(mainFileBaseAndExtension[0] + ".mjs");
            // noinspection ResultOfMethodCallIgnored
            allFilesArray[0].renameTo(mainFileWithMjsExtension);
            Source mainSource = Source.newBuilder(ID, mainFileWithMjsExtension).build();
            Value v = context.eval(mainSource);
            commonCheck(v);
        } finally {
            deleteFiles(allFilesArray);
        }
    }

    /**
     * Test that default function export and import works.
     */
    @Test
    public void testDefaultFunctionExport() throws IOException {
        File[] allFilesArray = null;
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {
            allFilesArray = prepareTestFileAndModules(
                            "resources/defaultfunctionexporttest.js",
                            "resources/diagmodule.js");
            Source mainSource = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value v = context.eval(mainSource);
            commonCheck(v);
        } finally {
            deleteFiles(allFilesArray);
        }
    }

    /**
     * Test that functions can be renamed for export.
     */
    @Test
    public void testRenamedExport() throws IOException {
        File[] allFilesArray = null;
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {

            allFilesArray = prepareTestFileAndModules(
                            "resources/renamedexporttest.js",
                            "resources/renamedexportmodule.js");
            Source mainSource = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value v = context.eval(mainSource);
            commonCheck(v);
        } finally {
            deleteFiles(allFilesArray);
        }
    }

    /**
     * Test that JavaScript classes can be exported.
     */
    @Test
    public void testClassExport() throws IOException {
        File[] allFilesArray = null;
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {
            allFilesArray = prepareTestFileAndModules(
                            "resources/classexporttest.js",
                            "resources/classexportmodule.js");
            Source mainSource = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value v = context.eval(mainSource);
            commonCheck(v);
        } finally {
            deleteFiles(allFilesArray);
        }
    }

    /**
     * Test that default class export works.
     */
    @Test
    public void testDefaultClassExport() throws IOException {
        File[] allFilesArray = null;
        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.ALL).build()) {
            allFilesArray = prepareTestFileAndModules(
                            "resources/defaultclassexporttest.js",
                            "resources/mymathmodule.js");
            Source mainSource = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value v = context.eval(mainSource);
            commonCheck(v);
        } finally {
            deleteFiles(allFilesArray);
        }
    }

    /**
     * Test that module file extensions do not have to be specified for import when custom file
     * system is used that adds the necessary extension when looking up the module file.
     */
    @Test
    public void testImportWithCustomFileSystem() throws IOException {
        File[] allFilesArray = prepareTestFileAndModules(
                        "resources/importwithcustomfilesystemtest.js",
                        "resources/functionexportmodule.js");

        FileSystem fileSystem = new FileSystem() {
            final java.nio.file.FileSystem fullIO = FileSystems.getDefault();

            @Override
            public Path parsePath(URI uri) {
                return fullIO.provider().getPath(uri);
            }

            @Override
            public Path parsePath(String path) {
                if (!Files.exists(Paths.get(path)) && !path.endsWith(".js")) {
                    String expected = allFilesArray[1].getAbsolutePath();
                    return fullIO.getPath(expected);
                }
                return fullIO.getPath(path);
            }

            @Override
            public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
                if (linkOptions.length > 0) {
                    throw new UnsupportedOperationException("CheckAccess for this FileSystem is unsupported with non-empty link options.");
                }
                fullIO.provider().checkAccess(path, modes.toArray(new AccessMode[]{}));
            }

            @Override
            public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
                fullIO.provider().createDirectory(dir, attrs);
            }

            @Override
            public void delete(Path path) throws IOException {
                fullIO.provider().delete(path);
            }

            @Override
            public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
                            FileAttribute<?>... attrs) throws IOException {
                return fullIO.provider().newByteChannel(path, options, attrs);
            }

            @Override
            public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
                return fullIO.provider().newDirectoryStream(dir, filter);
            }

            @Override
            public Path toAbsolutePath(Path path) {
                return path.toAbsolutePath();
            }

            @Override
            public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
                return path.toRealPath(linkOptions);
            }

            @Override
            public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
                return fullIO.provider().readAttributes(path, attributes, options);
            }
        };

        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().fileSystem(fileSystem).build()).build()) {
            Source mainSource = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value v = context.eval(mainSource);
            commonCheck(v);
        } finally {
            deleteFiles(allFilesArray);
        }

    }

    /**
     * Calling <code>eval()</code> on a ES module with option <code>esm-eval-returns-exports</code>
     * enabled returns the module exports.
     */
    @Test
    public void testExportNamespace() throws IOException {
        try (Context context = JSTest.newContextBuilder().option(ESM_EVAL_RETURNS_EXPORTS_NAME, "true").build()) {
            Source source = Source.newBuilder("js", "export const foo = 42; export var bar = 43", "test").mimeType(MODULE_MIME_TYPE).build();
            Value exports = context.eval(source);
            Assert.assertTrue(exports.hasMember("foo"));
            Assert.assertTrue(exports.getMember("foo").fitsInInt());
            Assert.assertEquals(42, exports.getMember("foo").asInt());
            Assert.assertTrue(exports.hasMember("bar"));
            Assert.assertTrue(exports.getMember("bar").fitsInInt());
            Assert.assertEquals(43, exports.getMember("bar").asInt());
        }
    }

    /**
     * Calling <code>eval()</code> on a ES module with option <code>esm-eval-returns-exports</code>
     * enabled returns the default namespace.
     */
    @Test
    public void testExportNamespaceDefault() throws IOException {
        try (Context context = JSTest.newContextBuilder().option(ESM_EVAL_RETURNS_EXPORTS_NAME, "true").build()) {
            Source source = Source.newBuilder("js", "export default 'foo';", "test").mimeType(MODULE_MIME_TYPE).build();
            Value exports = context.eval(source);
            Assert.assertTrue(exports.hasMember("default"));
            Assert.assertTrue(exports.getMember("default").isString());
            Assert.assertEquals("foo", exports.getMember("default").asString());
        }
    }

    /**
     * When <code>esm-eval-returns-exports</code> is enabled, modules do not return the last
     * expression.
     */
    @Test
    public void testExportNoExport() throws IOException {
        Source source = Source.newBuilder("js", "'foo';", "test").mimeType(MODULE_MIME_TYPE).build();
        try (Context context = JSTest.newContextBuilder().build()) {
            Value exports = context.eval(source);
            Assert.assertTrue(exports.isString());
            Assert.assertEquals("foo", exports.asString());
        }
        try (Context context = JSTest.newContextBuilder().option(ESM_EVAL_RETURNS_EXPORTS_NAME, "true").build()) {
            Value exports = context.eval(source);
            Assert.assertTrue(exports.hasMembers());
            Assert.assertEquals(0, exports.getMemberKeys().size());
        }
    }

    /**
     * ES module namespace is returned only for the root module, and nested imports work as
     * expected.
     */
    @Test
    public void testNestedImportNamespace() throws IOException {
        File[] allFilesArray = null;
        try (Context context = JSTest.newContextBuilder().option(ESM_EVAL_RETURNS_EXPORTS_NAME, "true").allowIO(IOAccess.ALL).build()) {
            allFilesArray = prepareTestFileAndModules(
                            "resources/importexport.js",
                            "resources/mymathmodule.js");
            Source source = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value exports = context.eval(source);
            Assert.assertTrue(exports.hasMembers());
            Assert.assertTrue(exports.hasMember("sqrtPlusOne"));
            Value sqrtPlusOne = exports.getMember("sqrtPlusOne");
            Value result = sqrtPlusOne.execute(121);
            Assert.assertTrue(result.fitsInInt());
            Assert.assertEquals(12, result.asInt());
        }
    }

    /**
     * When <code>esm-eval-returns-exports</code> is enabled, exports from ES modules with top-level
     * await are returned as well.
     */
    @Test
    public void testTopLevelAwait() throws IOException {
        try (Context context = JSTest.newContextBuilder().option(ESM_EVAL_RETURNS_EXPORTS_NAME, "true").build()) {
            Source source = Source.newBuilder("js", "export var async = await Promise.resolve('resolved!');", "test").mimeType(MODULE_MIME_TYPE).build();
            Value exports = context.eval(source);
            Assert.assertTrue(exports.hasMember("async"));
            Assert.assertTrue(exports.getMember("async").isString());
            Assert.assertEquals("resolved!", exports.getMember("async").asString());
        }
    }

    /**
     * When <code>esm-eval-returns-exports</code> is enabled, exports from ES modules with top-level
     * await are returned as well.
     */
    @Test
    public void testTopLevelAwaitImports() throws IOException {
        File[] allFilesArray = null;
        try (Context context = JSTest.newContextBuilder().option(ESM_EVAL_RETURNS_EXPORTS_NAME, "true").allowIO(IOAccess.ALL).build()) {
            allFilesArray = prepareTestFileAndModules(
                            "resources/importexporttlawait.js",
                            "resources/classexportmodule.js");
            Source source = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value exports = context.eval(source);
            Assert.assertTrue(exports.hasMembers());
            Assert.assertTrue(exports.hasMember("sqrtPlusOne"));
            Value sqrtPlusOne = exports.getMember("sqrtPlusOne");
            Value result = sqrtPlusOne.execute(121);
            Assert.assertTrue(result.fitsInInt());
            Assert.assertEquals(14, result.asInt());
        }
    }

    @Test
    public void testBareModulesVirtualFsAccesses() throws IOException {
        final String expectedBarePath = "bare.js";
        final File[] allFilesArray = prepareTestFileAndModules(
                        "resources/importbarevirtualfs.js",
                        "resources/folder/subfolder.js/foo.js");

        FileSystem fileSystem = new FileSystem() {
            final java.nio.file.FileSystem fullIO = FileSystems.getDefault();

            @Override
            public Path parsePath(URI uri) {
                return fullIO.provider().getPath(uri);
            }

            @Override
            public Path parsePath(String path) {
                if (expectedBarePath.equals(path)) {
                    String replacement = allFilesArray[1].getAbsolutePath();
                    return Paths.get(replacement);
                }
                return fullIO.getPath(path);
            }

            @Override
            public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
                if (linkOptions.length > 0) {
                    throw new UnsupportedOperationException("CheckAccess for this FileSystem is unsupported with non-empty link options.");
                }
                fullIO.provider().checkAccess(path, modes.toArray(new AccessMode[]{}));
            }

            @Override
            public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
                fullIO.provider().createDirectory(dir, attrs);
            }

            @Override
            public void delete(Path path) throws IOException {
                fullIO.provider().delete(path);
            }

            @Override
            public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
                            FileAttribute<?>... attrs) throws IOException {
                return fullIO.provider().newByteChannel(path, options, attrs);
            }

            @Override
            public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
                return fullIO.provider().newDirectoryStream(dir, filter);
            }

            @Override
            public Path toAbsolutePath(Path path) {
                return path.toAbsolutePath();
            }

            @Override
            public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
                return path.toRealPath(linkOptions);
            }

            @Override
            public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
                return fullIO.provider().readAttributes(path, attributes, options);
            }
        };

        try (Context context = JSTest.newContextBuilder().allowIO(IOAccess.newBuilder().fileSystem(fileSystem).build()).build()) {
            Source mainSource = Source.newBuilder(ID, allFilesArray[0]).mimeType(MODULE_MIME_TYPE).build();
            Value v = context.eval(mainSource);
            assertTrue(v.isString());
            assertEquals("HELLO GRAALJS", v.asString());
        } finally {
            deleteFiles(allFilesArray);
        }
    }
}
