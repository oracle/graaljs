/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader.DOT_DOT_SLASH;
import static com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader.DOT_SLASH;
import static com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader.SLASH;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSEngine;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import static com.oracle.truffle.js.runtime.JSContextOptions.ModuleLoaderFactoryMode.HANDLER;

public final class CommonJSResolution {

    public static final String FILE = "file";
    public static final String NODE_MODULES = "node_modules";
    public static final String PACKAGE_JSON = "package.json";
    public static final String INDEX_JS = "index.js";
    public static final String INDEX_JSON = "index.json";
    public static final String INDEX_NODE = "index.node";

    public static final String JS_EXT = ".js";
    public static final String CJS_EXT = ".cjs";
    public static final String MJS_EXT = ".mjs";
    public static final String JSON_EXT = ".json";
    public static final String NODE_EXT = ".node";

    private static final String NODE_PREFIX = "node:";

    private CommonJSResolution() {
    }

    public static String getCoreModuleReplacement(JSRealm realm, String moduleIdentifier) {
        String identifier = moduleIdentifier.startsWith(NODE_PREFIX) ? moduleIdentifier.substring(NODE_PREFIX.length()) : moduleIdentifier;
        return realm.getContextOptions().getCommonJSRequireBuiltins().get(identifier);
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
    static TruffleFile resolve(JSRealm realm, String moduleIdentifier, TruffleFile entryPath) {
        // 1. If X is an empty module
        if (moduleIdentifier.isEmpty()) {
            return null;
        }
        TruffleLanguage.Env env = realm.getEnv();
        // 2. If X begins with '/'
        TruffleFile currentWorkingPath = entryPath;
        if (moduleIdentifier.charAt(0) == '/') {
            currentWorkingPath = getFileSystemRootPath(env);
        }
        // 3. If X begins with './' or '/' or '../'
        if (isPathFileName(moduleIdentifier)) {
            TruffleFile module = loadAsFileOrDirectory(realm, joinPaths(currentWorkingPath, moduleIdentifier));
            // XXX(db) The Node.js informal spec says we should throw if module is null here.
            // Node v12.x, however, does not throw and attempts to load as a folder.
            if (module != null) {
                return module;
            }
        }
        // 4. 5. 6. Try loading as a folder, or throw if not existing
        return loadNodeModulesOrSelfReference(realm, moduleIdentifier, currentWorkingPath);
    }

    private static TruffleFile loadNodeModulesOrSelfReference(JSRealm realm, String moduleIdentifier, TruffleFile startFolder) {
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
            TruffleFile module = loadAsFileOrDirectory(realm, joinPaths(s, moduleIdentifier));
            if (module != null) {
                return module;
            }
        }
        return null;
    }

    public static TruffleFile loadIndex(TruffleFile modulePath) {
        /* @formatter:off
         *
         * LOAD_INDEX(X)
         *     1. If X/index.js is a file, load X/index.js as JavaScript text. STOP
         *     2. If X/index.json is a file, parse X/index.json to a JavaScript object. STOP
         *     3. If X/index.node is a file, load X/index.node as binary addon. STOP
         *
         * @formatter:on
         */
        TruffleFile indexJs = joinPaths(modulePath, INDEX_JS);
        if (fileExists(indexJs)) {
            return indexJs;
        }
        TruffleFile indexJson = joinPaths(modulePath, INDEX_JSON);
        if (fileExists(indexJson)) {
            return indexJson;
        } else if (fileExists(joinPaths(modulePath, INDEX_NODE))) {
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
        TruffleFile moduleJs = env.getPublicTruffleFile(modulePath.toString() + JS_EXT);
        if (fileExists(moduleJs)) {
            return moduleJs;
        }
        TruffleFile moduleJson = env.getPublicTruffleFile(modulePath.toString() + JSON_EXT);
        if (fileExists(moduleJson)) {
            return moduleJson;
        }
        if (fileExists(env.getPublicTruffleFile(modulePath.toString() + NODE_EXT))) {
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
        TruffleFile packageJson = joinPaths(modulePath, PACKAGE_JSON);
        if (fileExists(packageJson)) {
            JSDynamicObject jsonObj = loadJsonObject(packageJson, realm);
            if (JSDynamicObject.isJSDynamicObject(jsonObj)) {
                Object main = JSObject.get(jsonObj, Strings.PACKAGE_JSON_MAIN_PROPERTY_NAME);
                if (!(main instanceof TruffleString mainStr)) {
                    return loadIndex(modulePath);
                }
                TruffleFile module = joinPaths(modulePath, mainStr.toJavaStringUncached());
                TruffleFile asFile = loadAsFile(realm.getEnv(), module);
                if (asFile != null) {
                    return asFile;
                } else {
                    return loadIndex(module);
                }
            }
        } else {
            return loadIndex(modulePath);
        }
        return null;
    }

    public static JSDynamicObject loadJsonObject(TruffleFile jsonFile, JSRealm realm) {
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
                JSFunctionObject parse = (JSFunctionObject) realm.getJsonParseFunctionObject();
                TruffleString jsonString = Strings.fromJavaString(source.getCharacters().toString());
                Object jsonObj = JSFunction.call(JSArguments.create(Undefined.instance, parse, jsonString));
                if (JSDynamicObject.isJSDynamicObject(jsonObj)) {
                    return (JSDynamicObject) jsonObj;
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

    private static boolean isPathFileName(String moduleIdentifier) {
        return moduleIdentifier.startsWith(SLASH) || moduleIdentifier.startsWith(DOT_SLASH) || moduleIdentifier.startsWith(DOT_DOT_SLASH);
    }

    public static TruffleFile joinPaths(TruffleFile p1, String p2) {
        Objects.requireNonNull(p1);
        try {
            return p1.resolve(p2).getAbsoluteFile().normalize();
        } catch (InvalidPathException e) {
            throw CommonJSRequireBuiltin.fail(p2);
        }
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
