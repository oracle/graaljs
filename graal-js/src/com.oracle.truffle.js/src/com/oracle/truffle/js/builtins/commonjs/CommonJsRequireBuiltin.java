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
package com.oracle.truffle.js.builtins.commonjs;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.List;

public abstract class CommonJsRequireBuiltin extends GlobalBuiltins.JSFileLoadingOperation {

    private static final boolean LOG_REQUIRE_PATH_RESOLUTION = true;
    private static final Stack<String> requireDebugStack;

    static {
        requireDebugStack = LOG_REQUIRE_PATH_RESOLUTION ? new Stack<>() : null;
    }

    private static void log(Object... message) {
        if (LOG_REQUIRE_PATH_RESOLUTION) {
            StringBuilder s = new StringBuilder("['.'");
            for (String module : requireDebugStack) {
                s.append(" '").append(module).append("'");
            }
            s.append("] ");
            for (Object m : message) {
                s.append(m.toString());
            }
            System.err.println(s.toString());
        }
    }

    private static void debugStackPush(String moduleIdentifier) {
        if (LOG_REQUIRE_PATH_RESOLUTION) {
            requireDebugStack.push(moduleIdentifier);
        }
    }

    private static void debugStackPop() {
        if (LOG_REQUIRE_PATH_RESOLUTION) {
            requireDebugStack.pop();
        }
    }

    public static final String FILENAME_VAR_NAME = "__filename";
    public static final String DIRNAME_VAR_NAME = "__dirname";
    public static final String MODULE_PROPERTY_NAME = "module";
    public static final String EXPORTS_PROPERTY_NAME = "exports";
    public static final String REQUIRE_PROPERTY_NAME = "require";

    private static final String MODULE_END = "\n});";
    private static final String MODULE_PREAMBLE = "(function (exports, require, module, __filename, __dirname) {";

    private static final String LOADED_PROPERTY_NAME = "loaded";
    private static final String FILENAME_PROPERTY_NAME = "filename";
    private static final String ID_PROPERTY_NAME = "id";
    private static final String MAIN_PROPERTY_NAME = "main";
    private static final String ENV_PROPERTY_NAME = "env";

    private static final String JS_EXT = ".js";
    private static final String JSON_EXT = ".json";
    private static final String NODE_EXT = ".node";
    private static final String INDEX_JS = "index.js";
    private static final String INDEX_JSON = "index.json";
    private static final String INDEX_NODE = "index.node";
    private static final String PACKAGE_JSON = "package.json";
    private static final String NODE_MODULES = "node_modules";

    private static final String[] CORE_MODULES = new String[]{"assert", "async_hooks", "buffer", "child_process", "cluster", "crypto",
                    "dgram", "dns", "domain", "events", "fs", "http", "http2", "https", "module", "net",
                    "os", "path", "perf_hooks", "punycode", "querystring", "readline", "repl",
                    "stream", "string_decoder", "tls", "trace_events", "tty", "url", "util",
                    "v8", "vm", "worker_threads", "zlib"};

    private final TruffleFile modulesResolutionCwd;

    public static TruffleFile getModuleResolveCurrentWorkingDirectory(JSContext context) {
        String cwdOption = context.getContextOptions().getRequireCwd();
        return getModulesResolutionCwd(cwdOption, context.getRealm().getEnv());
    }

    CommonJsRequireBuiltin(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
        this.modulesResolutionCwd = getModuleResolveCurrentWorkingDirectory(context);
    }

    @Specialization
    protected Object require(VirtualFrame frame, String moduleIdentifier) {
        DynamicObject currentRequire = (DynamicObject) JSArguments.getFunctionObject(frame.getArguments());
        TruffleLanguage.Env env = getContext().getRealm().getEnv();
        TruffleFile resolutionEntryPath = getModuleResolutionEntryPath(currentRequire, env);
        return requireImpl(env, moduleIdentifier, resolutionEntryPath);
    }

    /* @formatter:off
     *
     * CommonJs `require` implementation based on the Node.js runtime loading mechanism as described
     * in the Node.js documentation: https://nodejs.org/api/modules.html.
     *
     * 1. If X is a core module
     *     a. return the core module
     *     b. STOP
     *
     * 2. If X begins with '/'
     *     a. set Y to be the filesystem root
     *
     * 3. If X begins with './' or '/' or '../'
     *     a. LOAD_AS_FILE(Y + X)
     *     b. LOAD_AS_DIRECTORY(Y + X)
     *     c. THROW "not found"
     *
     * 4. LOAD_NODE_MODULES(X, dirname(Y))
     * 5. LOAD_SELF_REFERENCE(X, dirname(Y))
     * 6. THROW "not found"
     *
     *  @formatter:on
     */
    @CompilerDirectives.TruffleBoundary
    private Object requireImpl(TruffleLanguage.Env env, String moduleIdentifier, TruffleFile currentWorkingPath) {
        log("required module '", moduleIdentifier, "'                                core:", isCoreModule(moduleIdentifier), " from path ", currentWorkingPath.normalize());
        // 1. If X is a core module
        if (isCoreModule(moduleIdentifier) || "".equals(moduleIdentifier)) {
            String moduleReplacementName = getContext().getContextOptions().getCommonJsRequireBuiltins().get(moduleIdentifier);
            if (moduleReplacementName != null && !"".equals(moduleReplacementName)) {
                return requireImpl(env, moduleReplacementName, modulesResolutionCwd);
            }
            throw fail(moduleIdentifier);
        }
        // 2. If X begins with '/'
        if (moduleIdentifier.charAt(0) == '/') {
            currentWorkingPath = getFileSystemRootPath(env);
        }
        // 3. If X begins with './' or '/' or '../'
        if (isPathFileName(moduleIdentifier)) {
            Object module = loadAsFileOrDirectory(env, joinPaths(env, currentWorkingPath, moduleIdentifier), moduleIdentifier);
            // XXX(db) The Node.js informal spec says we should throw if module is null here.
            // Node v12.x, however, does not throw and attempts to load as a folder.
            if (module != null) {
                return module;
            }
        }
        // 4. 5. 6. Try loading as a folder, or throw if not existing
        return loadNodeModulesOrSelfReference(env, moduleIdentifier, currentWorkingPath);
    }

    private Object loadNodeModulesOrSelfReference(TruffleLanguage.Env env, String moduleIdentifier, TruffleFile startFolder) {
        /* @formatter:off
         *
         * 1. let DIRS = NODE_MODULES_PATHS(START)
         * 2. for each DIR in DIRS:
         *     a. LOAD_AS_FILE(DIR/X)
         *     b. LOAD_AS_DIRECTORY(DIR/X)
         *
         * @formatter:on
         */
        List<TruffleFile> nodeModulesPaths = getNodeModulesPaths(env, startFolder);
        for (TruffleFile s : nodeModulesPaths) {
            Object module = loadAsFileOrDirectory(env, joinPaths(env, s, moduleIdentifier), moduleIdentifier);
            if (module != null) {
                return module;
            }
        }
        throw fail(moduleIdentifier);
    }

    private Object loadAsJavaScriptText(TruffleFile modulePath, String moduleIdentifier) {
        JSRealm realm = getContext().getRealm();
        // If cached, return from cache. This is by design to avoid infinite require loops.
        Map<TruffleFile, DynamicObject> commonJsCache = realm.getCommonJsRequireCache();
        if (commonJsCache.containsKey(modulePath.normalize())) {
            DynamicObject moduleBuiltin = commonJsCache.get(modulePath.normalize());
            Object cached = JSObject.get(moduleBuiltin, EXPORTS_PROPERTY_NAME);
            log("returning cached '", modulePath.normalize(), "'  APIs: {", JSObject.enumerableOwnNames((DynamicObject) cached), "}");
            return cached;
        }
        // Read the file.
        Source source = sourceFromPath(modulePath.toString(), realm);
        String filenameBuiltin = modulePath.normalize().toString();
        if (modulePath.getParent() == null) {
            throw fail(moduleIdentifier);
        }
        // Create `require` and other builtins for this module.
        String dirnameBuiltin = modulePath.getParent().getAbsoluteFile().normalize().toString();
        DynamicObject exportsBuiltin = createExportsBuiltin(realm);
        DynamicObject moduleBuiltin = createModuleBuiltin(realm, exportsBuiltin, filenameBuiltin);
        DynamicObject requireBuiltin = createRequireBuiltin(realm, moduleBuiltin, filenameBuiltin);
        DynamicObject env = JSUserObject.create(getContext());
        JSObject.set(env, ENV_PROPERTY_NAME, JSUserObject.create(getContext()));
        // Parse the module
        CharSequence characters = MODULE_PREAMBLE + source.getCharacters() + MODULE_END;
        Source moduleSources = Source.newBuilder(JavaScriptLanguage.ID).mimeType(JavaScriptLanguage.TEXT_MIME_TYPE).name(filenameBuiltin).content(characters).build();
        CallTarget moduleCallTarget = realm.getEnv().parsePublic(moduleSources);
        Object moduleExecutableFunction = moduleCallTarget.call();
        // Execute the module.
        if (JSFunction.isJSFunction(moduleExecutableFunction)) {
            commonJsCache.put(modulePath.normalize(), moduleBuiltin);
            try {
                debugStackPush(moduleIdentifier);
                log("executing '", filenameBuiltin, "' for ", moduleIdentifier);
                JSFunction.call(JSArguments.create(moduleExecutableFunction, moduleExecutableFunction, exportsBuiltin, requireBuiltin, moduleBuiltin, filenameBuiltin, dirnameBuiltin, env));
                JSObject.set(moduleBuiltin, LOADED_PROPERTY_NAME, true);
                return JSObject.get(moduleBuiltin, EXPORTS_PROPERTY_NAME);
            } catch (Exception e) {
                log("EXCEPTION: '", e.getMessage(), "'");
                throw e;
            } finally {
                debugStackPop();
                Object module = JSObject.get(moduleBuiltin, EXPORTS_PROPERTY_NAME);
                log("done '", moduleIdentifier, "' module.exports: ", module, "   APIs: {", JSObject.enumerableOwnNames((DynamicObject) module), "}");
            }
        }
        return null;
    }

    private Object loadAsFileOrDirectory(TruffleLanguage.Env env, TruffleFile modulePath, String moduleIdentifier) {
        Object maybeFile = loadAsFile(env, modulePath, moduleIdentifier);
        if (maybeFile == null) {
            return loadAsDirectory(env, modulePath, moduleIdentifier);
        } else {
            return maybeFile;
        }
    }

    private DynamicObject loadJsonObject(TruffleFile jsonFile) {
        try {
            if (fileExists(jsonFile)) {
                Source source = null;
                JSRealm realm = getContext().getRealm();
                TruffleFile file = GlobalBuiltins.resolveRelativeFilePath(jsonFile.toString(), realm.getEnv());
                if (file.isRegularFile()) {
                    source = sourceFromTruffleFile(file);
                } else {
                    throw fail(jsonFile.toString());
                }
                DynamicObject parse = (DynamicObject) realm.getJsonParseFunctionObject();
                assert source != null;
                String jsonString = source.getCharacters().toString().replace('\n', ' ');
                Object jsonObj = JSFunction.call(JSArguments.create(parse, parse, jsonString));
                if (JSObject.isJSObject(jsonObj)) {
                    return (DynamicObject) jsonObj;
                }
            }
            throw fail(jsonFile.toString());
        } catch (SecurityException e) {
            throw Errors.createErrorFromException(e);
        }
    }

    private Object loadAsDirectory(TruffleLanguage.Env env, TruffleFile modulePath, String moduleIdentifier) {
        TruffleFile packageJson = joinPaths(env, modulePath, PACKAGE_JSON);
        if (fileExists(packageJson)) {
            DynamicObject jsonObj = loadJsonObject(packageJson);
            if (JSObject.isJSObject(jsonObj)) {
                Object main = JSObject.get(jsonObj, MAIN_PROPERTY_NAME);
                if (!JSRuntime.isString(main)) {
                    return loadIndex(env, modulePath, moduleIdentifier);
                }
                TruffleFile module = joinPaths(env, modulePath, JSRuntime.safeToString(main));
                Object asFile = loadAsFile(env, module, moduleIdentifier);
                if (asFile != null) {
                    return asFile;
                } else {
                    Object loadIndex = loadIndex(env, module, moduleIdentifier);
                    if (loadIndex != null) {
                        return loadIndex;
                    }
                }
            }
        } else {
            return loadIndex(env, modulePath, moduleIdentifier);
        }
        throw fail(modulePath.toString());
    }

    private Object loadIndex(TruffleLanguage.Env env, TruffleFile modulePath, String moduleIdentifier) {
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
        TruffleFile indexJson = joinPaths(env, modulePath, INDEX_JSON);
        if (fileExists(indexJs)) {
            return loadAsJavaScriptText(indexJs, moduleIdentifier);
        } else if (fileExists(indexJson)) {
            return loadJsonObject(indexJson);
        } else if (fileExists(joinPaths(env, modulePath, INDEX_NODE))) {
            throw fail(modulePath.toString());
        }
        return null;
    }

    private Object loadAsFile(TruffleLanguage.Env env, TruffleFile modulePath, String moduleIdentifier) {
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
            return loadAsTextOrObject(modulePath, moduleIdentifier);
        }
        TruffleFile moduleJs = env.getPublicTruffleFile(modulePath.toString() + JS_EXT);
        if (fileExists(moduleJs)) {
            return loadAsJavaScriptText(moduleJs, moduleIdentifier);
        }
        TruffleFile moduleJson = env.getPublicTruffleFile(modulePath.toString() + JSON_EXT);
        if (fileExists(moduleJson)) {
            return loadJsonObject(moduleJson);
        }
        if (fileExists(env.getPublicTruffleFile(modulePath.toString() + NODE_EXT))) {
            throw fail(modulePath.toString());
        }
        return null;
    }

    private Object loadAsTextOrObject(TruffleFile file, String moduleIdentifier) {
        assert fileExists(file);
        String fileName = file.getName();
        if (hasExtension(fileName, JSON_EXT)) {
            return loadJsonObject(file);
        } else {
            return loadAsJavaScriptText(file, moduleIdentifier);
        }
    }

    private static JSException fail(String moduleIdentifier) {
        return JSException.create(JSErrorType.TypeError, "Cannot load CommonJs module: '" + moduleIdentifier + "'");
    }

    private DynamicObject createModuleBuiltin(JSRealm realm, DynamicObject exportsBuiltin, String fileNameBuiltin) {
        DynamicObject module = JSUserObject.create(realm.getContext(), realm);
        JSObject.set(module, EXPORTS_PROPERTY_NAME, exportsBuiltin);
        JSObject.set(module, ID_PROPERTY_NAME, fileNameBuiltin);
        JSObject.set(module, FILENAME_PROPERTY_NAME, fileNameBuiltin);
        JSObject.set(module, LOADED_PROPERTY_NAME, false);
        return module;
    }

    private DynamicObject createRequireBuiltin(JSRealm realm, DynamicObject moduleBuiltin, String fileNameBuiltin) {
        DynamicObject mainRequire = (DynamicObject) realm.getCommonJsRequireFunctionObject();
        JSFunctionData functionData = JSFunction.getFunctionData(mainRequire);
        DynamicObject newRequire = JSFunction.create(realm, functionData);
        JSObject.set(newRequire, MODULE_PROPERTY_NAME, moduleBuiltin);
        // XXX(db) Here, we store the current filename in the (new) require builtin.
        // In this way, we avoid managing a shadow stack to track the current require's parent.
        // In Node.js, this is done using a (closed) level variable.
        JSObject.set(newRequire, FILENAME_VAR_NAME, fileNameBuiltin);
        return newRequire;
    }

    private DynamicObject createExportsBuiltin(JSRealm realm) {
        return JSUserObject.create(realm.getContext(), realm);
    }

    private static TruffleFile getModulesResolutionCwd(String cwdOption, TruffleLanguage.Env env) {
        return cwdOption == null ? env.getCurrentWorkingDirectory() : env.getPublicTruffleFile(cwdOption);
    }

    private boolean fileExists(TruffleFile modulePath) {
        return modulePath.exists() && modulePath.isRegularFile();
    }

    private boolean isPathFileName(String moduleIdentifier) {
        if (moduleIdentifier.length() > 0 && moduleIdentifier.charAt(0) == '/') {
            return true;
        } else if (moduleIdentifier.length() > 1 && "./".equals(moduleIdentifier.substring(0, 2))) {
            return true;
        } else {
            return moduleIdentifier.length() > 2 && "../".equals(moduleIdentifier.substring(0, 3));
        }
    }

    private boolean isCoreModule(String moduleIdentifier) {
        return Arrays.asList(CORE_MODULES).contains(moduleIdentifier);
    }

    private static TruffleFile joinPaths(TruffleLanguage.Env env, TruffleFile p1, String p2) {
        String pathSeparator = env.getFileNameSeparator();
        String pathName = p1.normalize().toString();
        TruffleFile truffleFile = env.getPublicTruffleFile(pathName + pathSeparator + p2);
        return truffleFile.normalize();
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

    private List<TruffleFile> getNodeModulesPaths(TruffleLanguage.Env env, TruffleFile path) {
        List<TruffleFile> list = new ArrayList<>();
        List<TruffleFile> paths = getAllParentPaths(path);
        for (TruffleFile p : paths) {
            if (p.endsWith(NODE_MODULES)) {
                list.add(p);
            } else {
                String pathSeparator = env.getFileNameSeparator();
                TruffleFile truffleFile = env.getPublicTruffleFile(p.toString() + pathSeparator + NODE_MODULES);
                list.add(truffleFile);
            }
        }
        return list;
    }

    private TruffleFile getFileSystemRootPath(TruffleLanguage.Env env) {
        TruffleFile root = env.getCurrentWorkingDirectory();
        TruffleFile last = root;
        while (root != null) {
            last = root;
            root = root.getParent();
        }
        return last;
    }

    private TruffleFile getModuleResolutionEntryPath(DynamicObject currentRequire, TruffleLanguage.Env env) {
        if (JSObject.isJSObject(currentRequire)) {
            Object maybeFilename = JSObject.get(currentRequire, FILENAME_VAR_NAME);
            if (JSRuntime.isString(maybeFilename)) {
                String fileName = (String) maybeFilename;
                if (isFile(env, fileName)) {
                    return getParent(env, fileName);
                }
            }
            // dirname not a string. Use default cwd.
        }
        // This is not a nested `require()` call, so we use the default cwd.
        return modulesResolutionCwd;
    }

    private TruffleFile getParent(TruffleLanguage.Env env, String fileName) {
        return env.getPublicTruffleFile(fileName).getParent();
    }

    private boolean isFile(TruffleLanguage.Env env, String fileName) {
        return env.getPublicTruffleFile(fileName).exists();
    }

    private static boolean hasExtension(String fileName, String ext) {
        return fileName.lastIndexOf(ext) > 0 && fileName.lastIndexOf(ext) == fileName.length() - ext.length();
    }

}
