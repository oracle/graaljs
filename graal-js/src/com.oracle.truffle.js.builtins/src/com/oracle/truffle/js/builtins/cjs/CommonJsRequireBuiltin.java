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
package com.oracle.truffle.js.builtins.cjs;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class CommonJsRequireBuiltin extends GlobalBuiltins.JSLoadOperation {

    private static final String MODULE_CLOSURE = "\n});";
    private static final String MODULE_PREAMBLE = "(function (exports, require, module, __filename, __dirname) {";

    private static final Path ROOT_PATH = Paths.get(File.listRoots()[0].getAbsolutePath());;
    private static final String SEPARATOR = File.separator;
    private static final String INDEX_JS = "index.js";
    private static final String INDEX_JSON = "index.json";
    private static final String INDEX_NODE = "index.node";
    private static final String PACKAGE_JSON = "package.json";
    private static final String NODE_MODULES = "node_modules";

    private static final String JS_EXT = ".js";
    private static final String JSON_EXT = ".json";
    private static final String NODE_EXT = ".node";

    private static final String EXPORTS = "exports";
    private static final String REQUIRE = "require";
    private static final String MAIN_PROPERTY ="main";
    
    private static final String[] CORE_MODULES = new String[] {"assert", "async_hooks", "buffer", "child_process", "cluster", "crypto",
            "dgram", "dns", "domain", "events", "fs", "http", "http2", "https", "net",
            "os", "path", "perf_hooks", "punycode", "querystring", "readline", "repl",
            "stream", "string_decoder", "tls", "trace_events", "tty", "url", "util",
            "v8", "vm", "worker_threads", "zlib"};

    private final Path cwd;

    public CommonJsRequireBuiltin(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
        String cwdOption = context.getContextOptions().getRequireCwd();
        this.cwd = (cwdOption == null ? Paths.get(".").toAbsolutePath().normalize() : Paths.get(cwdOption));
    }

    @Specialization
    protected Object require(String moduleIdentifier) {
        Stack<Path> commonJsRequireStack = getContext().getRealm().getCommonJsRequireStack();
        Path y = commonJsRequireStack.isEmpty() ? cwd : commonJsRequireStack.get(commonJsRequireStack.size() - 1);
        /*
            1. If X is a core module,
                a. return the core module
                b. STOP
        */
        if (isCoreModule(moduleIdentifier) || "".equals(moduleIdentifier)) {
            throw fail(moduleIdentifier);
        }
        /*
            2. If X begins with '/'
                a. set Y to be the filesystem root
        */
        if (moduleIdentifier.charAt(0) == '/') {
            // XXX(db) this always returns `c:\` on windows.
            y = Paths.get(File.listRoots()[0].getAbsolutePath());
        }
        /*
            3. If X begins with './' or '/' or '../'
                a. LOAD_AS_FILE(Y + X)
                b. LOAD_AS_DIRECTORY(Y + X)
                c. THROW "not found"
        */
        if (isPathFileName(moduleIdentifier)) {
            Object module = loadAsFileOrDirectory(joinPaths(y, moduleIdentifier));
            if (module == null) {
                // XXX(db) node.js informal spec says we should throw. Node v12.x does not throw and continues.
                // throw fail(moduleIdentifier);
            } else {
                return module;
            }
        }
        /*
            4. LOAD_NODE_MODULES(X, dirname(Y))
            5. LOAD_SELF_REFERENCE(X, dirname(Y))
            6. THROW "not found"
        */
        Object module = loadNodeModulesOrSelfReference(moduleIdentifier, y);
        if (module == null) {
            throw fail(moduleIdentifier);
        } else {
            return module;
        }
    }

    private Object loadNodeModulesOrSelfReference(String moduleIdentifier, Path start) {
        /*
            1. let DIRS = NODE_MODULES_PATHS(START)
            2. for each DIR in DIRS:
                a. LOAD_AS_FILE(DIR/X)
                b. LOAD_AS_DIRECTORY(DIR/X)
        */
        Path[] nodeModulesPaths = getNodeModulesPaths(start);
        for (Path s : nodeModulesPaths) {
            Object module = loadAsFileOrDirectory(joinPaths(s, moduleIdentifier));
            if (module != null) {
                return module;
            }
        }
        throw fail(moduleIdentifier);
    }

    private static Path joinPaths(Path p1, String p2) {
        return Paths.get(p1.normalize().toString(), p2);
    }

    private Path[] getNodeModulesPaths(Path path) {
        File[] folders = path.toFile().listFiles(File::isDirectory);
        List<Path> list = new ArrayList<>();

        assert folders != null;
        for (File folder : folders) {
            // TODO skip node_modules folder
            list.add(folder.toPath());
        }
        // TODO the list should include "Global" folders.
        return list.toArray(new Path[]{});
    }

    private static JSException fail(String moduleIdentifier) {
        return JSException.create(JSErrorType.TypeError, "Cannot load Npm module: '" + moduleIdentifier + "'");
    }

    private Object loadAsJavaScriptText(Path modulePath) {
        JSRealm realm = getContext().getRealm();

        Map<Path, DynamicObject> commonJsCache = realm.getCommonJsRequireCache();
        if (commonJsCache.containsKey(modulePath.normalize())) {
            return commonJsCache.get(modulePath.normalize());
        }

        Source source = sourceFromPath(modulePath.toString(), realm);

        DynamicObject exportsBuiltin = createExportsBuiltin(realm);
        DynamicObject requireBuiltin = createRequireBuiltin(realm);
        DynamicObject moduleBuiltin = createModuleBuiltin(realm, exportsBuiltin);
        String filenameBuiltin = modulePath.getFileName().toString();
        String dirnameBuiltin = modulePath.getParent().toAbsolutePath().toString();

        CharSequence characters = MODULE_PREAMBLE + source.getCharacters() + MODULE_CLOSURE;
        Source build = Source.newBuilder(JavaScriptLanguage.ID)
                .mimeType(JavaScriptLanguage.TEXT_MIME_TYPE)
                .name(filenameBuiltin)
                .content(characters)
                .build();

        CallTarget callTarget = realm.getEnv().parsePublic(build);
        Object call = callTarget.call();

        if (JSFunction.isJSFunction(call)) {
            try {
                commonJsCache.put(modulePath.normalize(), exportsBuiltin);
                realm.getCommonJsRequireStack().push(Paths.get(dirnameBuiltin));
                JSFunction.call(JSArguments.create(call, call, exportsBuiltin, requireBuiltin, moduleBuiltin, filenameBuiltin, dirnameBuiltin));
                return exportsBuiltin;
            } finally {
                realm.getCommonJsRequireStack().pop();
            }
        }
        return null;
    }

    private Object loadAsFileOrDirectory(Path modulePath) {
        Object maybeFile = loadAsFile(modulePath);
        if (maybeFile == null) {
            return loadAsDirectory(modulePath);
        } else {
            return maybeFile;
        }
    }

    private DynamicObject loadJsonObject(Path jsonFile) {
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

    private Object loadAsDirectory(Path modulePath) {
        if (fileExists(Paths.get(modulePath.toString(), PACKAGE_JSON))) {
            DynamicObject jsonObj = loadJsonObject(Paths.get(modulePath.toString(), PACKAGE_JSON));
            if (JSObject.isJSObject(jsonObj)) {
                Object main = JSObject.get(jsonObj, MAIN_PROPERTY);
                if (!JSRuntime.isString(main)) {
                    return loadIndex(modulePath);
                }
                Path m = Paths.get(modulePath.toString(), JSRuntime.safeToString(main));
                Object asFile = loadAsFile(m);
                if (asFile != null) {
                    return asFile;
                } else {
                    Object loadIndex = loadIndex(m);
                    if (loadIndex != null) {
                        return loadIndex;
                    }
                }
            }
        } else {
            return loadIndex(modulePath);
        }
        throw fail(modulePath.toString());
    }

    private Object loadIndex(Path modulePath) {
        /*
            LOAD_INDEX(X)
                1. If X/index.js is a file, load X/index.js as JavaScript text.  STOP
                2. If X/index.json is a file, parse X/index.json to a JavaScript object. STOP
                3. If X/index.node is a file, load X/index.node as binary addon.  STOP
        */
        if (fileExists(Paths.get(modulePath.toString(), INDEX_JS))) {
            return loadAsJavaScriptText(Paths.get(modulePath.toString(), INDEX_JS));
        } else if (fileExists(Paths.get(modulePath.toString(), INDEX_JSON))) {
            return loadJsonObject(Paths.get(modulePath.toString(), INDEX_JSON));
        } else if (fileExists(Paths.get(modulePath.toString(), INDEX_NODE))) {
            throw fail(modulePath.toString());
        }
        return null;
    }

    private Object loadAsFile(Path modulePath) {
        /*
            LOAD_AS_FILE(X)
                1. If X is a file, load X as JavaScript text.  STOP
                2. If X.js is a file, load X.js as JavaScript text.  STOP
                3. If X.json is a file, parse X.json to a JavaScript Object.  STOP
                4. If X.node is a file, load X.node as binary addon.  STOP
        */
        if (fileExists(modulePath)) {
            return loadAsTextOrObject(modulePath);
        } else if (fileExists(Paths.get(modulePath.toString() + JS_EXT))) {
            return loadAsJavaScriptText(Paths.get(modulePath.toString() + JS_EXT));
        } else if (fileExists(Paths.get(modulePath.toString() + JSON_EXT))) {
            return loadJsonObject(Paths.get(modulePath.toString() + JSON_EXT));
        } else if (fileExists(Paths.get(modulePath.toString() + NODE_EXT))) {
            throw fail(modulePath.toString());
        }
        return null;
    }

    private Object loadAsTextOrObject(Path file) {
        assert fileExists(file);
        // TODO safer way to check ext
        String fileName = file.toFile().getName();
        int len = fileName.length();
        if (len > 5) {
            if (fileName.substring(len - 5, len).equals(".json")) {
                return loadJsonObject(file);
            } else {
                return loadAsJavaScriptText(file);
            }
        } else {
            return loadAsJavaScriptText(file);
        }
    }

    private boolean fileExists(Path modulePath) {
        return Files.exists(modulePath) && modulePath.toFile().isFile();
    }

    private boolean isPathFileName(String moduleIdentifier) {
        if (moduleIdentifier.charAt(0) == '/') {
            return true;
        } else if (moduleIdentifier.length() > 1 && moduleIdentifier.substring(0, 2).equals("./")) {
            return true;
        } else {
            return moduleIdentifier.length() > 2 && moduleIdentifier.substring(0, 3).equals("../");
        }
    }

    private boolean isCoreModule(String moduleIdentifier) {
        return Arrays.asList(CORE_MODULES).contains(moduleIdentifier);
    }

    private DynamicObject createModuleBuiltin(JSRealm realm, DynamicObject exportsBuiltin) {
        DynamicObject module = JSUserObject.create(realm.getContext(), realm);
        JSObject.set(module, EXPORTS, exportsBuiltin);
        return module;
    }

    private DynamicObject createRequireBuiltin(JSRealm realm) {
        return (DynamicObject) realm.getCommonJsRequireFunctionObject();
    }

    private DynamicObject createExportsBuiltin(JSRealm realm) {
        return JSUserObject.create(realm.getContext(), realm);
    }

}
