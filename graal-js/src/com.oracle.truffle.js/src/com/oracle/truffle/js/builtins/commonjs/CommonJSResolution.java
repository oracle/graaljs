/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.commonjs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

final class CommonJSResolution {

    public static final String NODE_MODULES = "node_modules";
    public static final TruffleString PACKAGE_JSON = Strings.constant("package.json");
    public static final TruffleString INDEX_JS = Strings.constant("index.js");
    public static final TruffleString INDEX_JSON = Strings.constant("index.json");
    public static final TruffleString INDEX_NODE = Strings.constant("index.node");

    /** Known node core modules. Array must be sorted! */
    public static final TruffleString[] CORE_MODULES = new TruffleString[]{
                    Strings.constant("assert"),
                    Strings.constant("async_hooks"),
                    Strings.constant("buffer"),
                    Strings.constant("child_process"),
                    Strings.constant("cluster"),
                    Strings.constant("console"),
                    Strings.constant("constants"),
                    Strings.constant("crypto"),
                    Strings.constant("dgram"),
                    Strings.constant("diagnostics_channel"),
                    Strings.constant("dns"),
                    Strings.constant("domain"),
                    Strings.constant("events"),
                    Strings.constant("fs"),
                    Strings.constant("http"),
                    Strings.constant("http2"),
                    Strings.constant("https"),
                    Strings.constant("inspector"),
                    Strings.constant("module"),
                    Strings.constant("net"),
                    Strings.constant("os"),
                    Strings.constant("path"),
                    Strings.constant("perf_hooks"),
                    Strings.constant("process"),
                    Strings.constant("punycode"),
                    Strings.constant("querystring"),
                    Strings.constant("readline"),
                    Strings.constant("repl"),
                    Strings.constant("stream"),
                    Strings.constant("string_decoder"),
                    Strings.constant("sys"),
                    Strings.constant("timers"),
                    Strings.constant("tls"),
                    Strings.constant("trace_events"),
                    Strings.constant("tty"),
                    Strings.constant("url"),
                    Strings.constant("util"),
                    Strings.constant("v8"),
                    Strings.constant("vm"),
                    Strings.constant("wasi"),
                    Strings.constant("worker_threads"),
                    Strings.constant("zlib")};

    static {
        assert IntStream.range(0, CORE_MODULES.length - 1).allMatch(i -> Strings.compareTo(CORE_MODULES[i], CORE_MODULES[i + 1]) <= 0);
    }

    private CommonJSResolution() {
    }

    static boolean isCoreModule(TruffleString moduleIdentifier) {
        return Arrays.binarySearch(CORE_MODULES, moduleIdentifier, Strings::compareTo) >= 0;
    }

    static String getCurrentFileNameFromStack() {
        Source callerSource = JSFunction.getCallerSource();
        if (callerSource != null) {
            return callerSource.getPath();
        }
        return null;
    }

    /**
     * CommonJS `require` implementation based on the Node.js runtime loading mechanism as described
     * in the Node.js documentation: https://nodejs.org/api/modules.html.
     *
     * @formatter:off
     *       1. If X is a core module
     *           a. return the core module
     *           b. STOP
     *
     *       2. If X begins with '/'
     *           a. set Y to be the filesystem root
     *
     *       3. If X begins with './' or '/' or '../'
     *           a. LOAD_AS_FILE(Y + X)
     *           b. LOAD_AS_DIRECTORY(Y + X)
     *           c. THROW "not found"
     *
     *       4. LOAD_NODE_MODULES(X, dirname(Y))
     *       5. LOAD_SELF_REFERENCE(X, dirname(Y))
     *       6. THROW "not found"
     *@formatter:on
     *
     * @param moduleIdentifier The module identifier to be resolved.
     * @param entryPath The initial directory from which the resolution algorithm is executed.
     * @return A {@link TruffleFile} for the module executable file, or {@code null} if the module cannot be resolved.
     */
    @CompilerDirectives.TruffleBoundary
    static TruffleFile resolve(JSRealm realm, TruffleString moduleIdentifier, TruffleFile entryPath) {
        // 1. If X is an empty module
        if (moduleIdentifier.isEmpty()) {
            return null;
        }
        TruffleLanguage.Env env = realm.getEnv();
        // 2. If X begins with '/'
        TruffleFile currentWorkingPath = entryPath;
        if (Strings.charAt(moduleIdentifier, 0) == '/') {
            currentWorkingPath = getFileSystemRootPath(env);
        }
        // 3. If X begins with './' or '/' or '../'
        if (isPathFileName(moduleIdentifier)) {
            TruffleFile module = loadAsFileOrDirectory(realm, joinPaths(env, currentWorkingPath, moduleIdentifier));
            // XXX(db) The Node.js informal spec says we should throw if module is null here.
            // Node v12.x, however, does not throw and attempts to load as a folder.
            if (module != null) {
                return module;
            }
        }
        // 4. 5. 6. Try loading as a folder, or throw if not existing
        return loadNodeModulesOrSelfReference(realm, moduleIdentifier, currentWorkingPath);
    }

    private static TruffleFile loadNodeModulesOrSelfReference(JSRealm realm, TruffleString moduleIdentifier, TruffleFile startFolder) {
        /* @formatter:off
         *
         * 1. let DIRS = NODE_MODULES_PATHS(START)
         * 2. for each DIR in DIRS:
         *     a. LOAD_AS_FILE(DIR/X)
         *     b. LOAD_AS_DIRECTORY(DIR/X)
         *
         * @formatter:on
         */
        List<TruffleFile> nodeModulesPaths = getNodeModulesPaths(startFolder);
        for (TruffleFile s : nodeModulesPaths) {
            TruffleFile module = loadAsFileOrDirectory(realm, joinPaths(realm.getEnv(), s, moduleIdentifier));
            if (module != null) {
                return module;
            }
        }
        return null;
    }

    public static TruffleFile loadIndex(TruffleLanguage.Env env, TruffleFile modulePath) {
        /* @formatter:off
         *
         * LOAD_INDEX(X)
         *     1. If X/index.js is a file, load X/index.js as JavaScript text. STOP
         *     2. If X/index.json is a file, parse X/index.json to a JavaScript object. STOP
         *     3. If X/index.node is a file, load X/index.node as binary addon. STOP
         *
         * @formatter:on
         */
        TruffleFile indexJs = joinPaths(env, modulePath, INDEX_JS);
        if (fileExists(indexJs)) {
            return indexJs;
        }
        TruffleFile indexJson = joinPaths(env, modulePath, INDEX_JSON);
        if (fileExists(indexJson)) {
            return indexJson;
        } else if (fileExists(joinPaths(env, modulePath, INDEX_NODE))) {
            // Ignore .node files.
            return null;
        }
        return null;
    }

    static TruffleFile loadAsFile(TruffleLanguage.Env env, TruffleFile modulePath) {
        /* @formatter:off
         *
         * LOAD_AS_FILE(X)
         *     1. If X is a file, load X as JavaScript text. STOP
         *     2. If X.js is a file, load X.js as JavaScript text. STOP
         *     3. If X.json is a file, parse X.json to a JavaScript Object. STOP
         *     4. If X.node is a file, load X.node as binary addon. STOP
         *
         * @formatter:on
         */
        if (fileExists(modulePath)) {
            return modulePath;
        }
        TruffleFile moduleJs = env.getPublicTruffleFile(modulePath.toString() + Strings.JS_EXT);
        if (fileExists(moduleJs)) {
            return moduleJs;
        }
        TruffleFile moduleJson = env.getPublicTruffleFile(modulePath.toString() + Strings.JSON_EXT);
        if (fileExists(moduleJson)) {
            return moduleJson;
        }
        if (fileExists(env.getPublicTruffleFile(modulePath.toString() + Strings.NODE_EXT))) {
            // .node files not supported.
            return null;
        }
        return null;
    }

    public static List<TruffleFile> getNodeModulesPaths(TruffleFile path) {
        List<TruffleFile> list = new ArrayList<>();
        List<TruffleFile> paths = getAllParentPaths(path);
        for (TruffleFile p : paths) {
            if (p.endsWith(NODE_MODULES)) {
                list.add(p);
            } else {
                TruffleFile truffleFile = p.resolve(NODE_MODULES);
                list.add(truffleFile);
            }
        }
        return list;
    }

    private static TruffleFile loadAsFileOrDirectory(JSRealm realm, TruffleFile modulePath) {
        TruffleFile maybeFile = loadAsFile(realm.getEnv(), modulePath);
        if (maybeFile == null) {
            return loadAsDirectory(realm, modulePath);
        } else {
            return maybeFile;
        }
    }

    private static List<TruffleFile> getAllParentPaths(TruffleFile from) {
        List<TruffleFile> paths = new ArrayList<>();
        TruffleFile p = from;
        while (p != null) {
            paths.add(p);
            p = p.getParent();
        }
        return paths;
    }

    private static TruffleFile loadAsDirectory(JSRealm realm, TruffleFile modulePath) {
        TruffleLanguage.Env env = realm.getEnv();
        TruffleFile packageJson = joinPaths(env, modulePath, PACKAGE_JSON);
        if (fileExists(packageJson)) {
            DynamicObject jsonObj = loadJsonObject(packageJson, realm);
            if (JSDynamicObject.isJSDynamicObject(jsonObj)) {
                Object main = JSObject.get(jsonObj, Strings.PACKAGE_JSON_MAIN_PROPERTY_NAME);
                if (!Strings.isTString(main)) {
                    return loadIndex(env, modulePath);
                }
                TruffleFile module = joinPaths(env, modulePath, JSRuntime.safeToString(main));
                TruffleFile asFile = loadAsFile(env, module);
                if (asFile != null) {
                    return asFile;
                } else {
                    return loadIndex(env, module);
                }
            }
        } else {
            return loadIndex(env, modulePath);
        }
        return null;
    }

    public static DynamicObject loadJsonObject(TruffleFile jsonFile, JSRealm realm) {
        try {
            if (fileExists(jsonFile)) {
                Source source = null;
                TruffleFile file = GlobalBuiltins.resolveRelativeFilePath(jsonFile.toString(), realm.getEnv());
                if (file.isRegularFile()) {
                    source = sourceFromTruffleFile(file);
                }
                if (source == null) {
                    return null;
                }
                DynamicObject parse = (DynamicObject) realm.getJsonParseFunctionObject();
                TruffleString jsonString = Strings.fromJavaString(source.getCharacters().toString());
                Object jsonObj = JSFunction.call(JSArguments.create(parse, parse, jsonString));
                if (JSDynamicObject.isJSDynamicObject(jsonObj)) {
                    return (DynamicObject) jsonObj;
                }
            }
            return null;
        } catch (SecurityException | IllegalArgumentException | UnsupportedOperationException e) {
            throw Errors.createErrorFromException(e);
        }
    }

    private static Source sourceFromTruffleFile(TruffleFile file) {
        try {
            return Source.newBuilder(JavaScriptLanguage.ID, file).build();
        } catch (IOException | SecurityException | IllegalArgumentException | UnsupportedOperationException e) {
            return null;
        }
    }

    public static boolean fileExists(TruffleFile modulePath) {
        return modulePath.exists() && modulePath.isRegularFile();
    }

    private static boolean isPathFileName(TruffleString moduleIdentifier) {
        return Strings.startsWith(moduleIdentifier, Strings.SLASH) || Strings.startsWith(moduleIdentifier, Strings.DOT_SLASH) || Strings.startsWith(moduleIdentifier, Strings.DOT_DOT_SLASH);
    }

    public static TruffleFile joinPaths(TruffleLanguage.Env env, TruffleFile p1, TruffleString p2) {
        Objects.requireNonNull(p1);
        String pathSeparator = env.getFileNameSeparator();
        String pathName = p1.normalize().toString();
        TruffleFile truffleFile = env.getPublicTruffleFile(pathName + pathSeparator + p2);
        return truffleFile.normalize();
    }

    private static TruffleFile getFileSystemRootPath(TruffleLanguage.Env env) {
        TruffleFile root = env.getCurrentWorkingDirectory();
        TruffleFile last = root;
        while (root != null) {
            last = root;
            root = root.getParent();
        }
        return last;
    }

}
