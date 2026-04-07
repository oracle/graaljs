/*
 * Copyright (c) 2019, 2026, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.CJS_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.JSON_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.JS_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.MJS_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.NODE_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.getCoreModuleReplacement;

import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.function.EvalNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class CommonJSRequireBuiltin extends GlobalBuiltins.JSFileLoadingOperation {

    private static final boolean LOG_REQUIRE_PATH_RESOLUTION = false;
    private static final Stack<String> requireDebugStack;

    private static final String MODULE_PREAMBLE_PREFIX = "(function (";
    private static final String MODULE_PREAMBLE_POST = ") {";
    private static final String MODULE_END = "\n});";
    private static final String MODULE_FUNCTION_ARGS = "exports, require, module, __filename, __dirname";
    private static final String[] MODULE_PARSE_ARGS = {
                    JavaScriptLanguage.NODE_ENV_PARSE_TOKEN,
                    MODULE_PREAMBLE_PREFIX + MODULE_FUNCTION_ARGS + MODULE_PREAMBLE_POST,
                    MODULE_END,
                    String.valueOf(false)
    };

    public static final String UNSUPPORTED_NODE_FILE = "Unsupported .node file: ";

    /** Used to store the {@link Source} of a CJS module in its module object. */
    public static final HiddenKey MODULE_SOURCE_KEY = new HiddenKey("Source");

    static {
        requireDebugStack = LOG_REQUIRE_PATH_RESOLUTION ? new Stack<>() : null;
    }

    public static void log(Object... message) {
        if (LOG_REQUIRE_PATH_RESOLUTION) {
            StringBuilder s = new StringBuilder("['.'");
            for (String module : requireDebugStack) {
                s.append(" '").append(module).append("'");
            }
            s.append("] ");
            for (Object m : message) {
                String desc;
                if (m == null) {
                    desc = "null";
                } else if (m instanceof JSDynamicObject) {
                    desc = "   APIs: {" + JSObject.enumerableOwnNames((JSDynamicObject) m) + "}";
                } else {
                    desc = m.toString();
                }
                s.append(desc);
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

    @TruffleBoundary
    static TruffleFile getModuleResolveCurrentWorkingDirectory(JSRealm realm) {
        String currentFileNameFromStack = CommonJSResolution.getCurrentFileNameFromStack();
        return getModuleResolveCurrentWorkingDirectory(realm, currentFileNameFromStack);
    }

    static TruffleFile getModuleResolveCurrentWorkingDirectory(JSRealm realm, String currentFileNameFromStack) {
        TruffleLanguage.Env env = realm.getEnv();
        if (currentFileNameFromStack != null) {
            TruffleFile truffleFile = env.getPublicTruffleFile(currentFileNameFromStack);
            if (truffleFile.isRegularFile() && truffleFile.getParent() != null) {
                return truffleFile.getParent().normalize();
            }
        }
        return getRequireCwd(realm, env);
    }

    static TruffleFile getRequireCwd(JSRealm realm, TruffleLanguage.Env env) {
        String cwdOption = realm.getContextOptions().getRequireCwd();
        return cwdOption.isEmpty() ? env.getCurrentWorkingDirectory() : env.getPublicTruffleFile(cwdOption);
    }

    CommonJSRequireBuiltin(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @TruffleBoundary
    @Specialization
    protected Object require(JSDynamicObject currentRequire, TruffleString moduleIdentifier) {
        JSRealm realm = getRealm();
        String moduleIdentifierJavaString = moduleIdentifier.toJavaStringUncached();
        try {
            TruffleFile resolutionEntryPath = getModuleResolutionEntryPath(currentRequire, realm);
            ScriptOrModule activeScriptOrModule = EvalNode.findActiveScriptOrModule(EvalNode.findCallNode(realm));
            JSObject module = requireImpl(moduleIdentifierJavaString, resolutionEntryPath, realm, activeScriptOrModule);
            return JSObject.get(module, Strings.EXPORTS_PROPERTY_NAME);
        } catch (SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
            throw fail(moduleIdentifierJavaString, e.getMessage());
        }
    }

    @Fallback
    protected static Object fallback(@SuppressWarnings("unused") Object function, Object moduleIdentifier) {
        throw Errors.createTypeErrorNotAString(moduleIdentifier);
    }

    @TruffleBoundary
    static JSObject requireImpl(String moduleIdentifier, TruffleFile entryPath, JSRealm realm, ScriptOrModule activeScriptOrModule) {
        log("required module '", moduleIdentifier, "' from path ", entryPath);
        String moduleReplacementName = getCoreModuleReplacement(realm, moduleIdentifier);
        if (moduleReplacementName != null) {
            log("using module replacement for module '", moduleIdentifier, "' with ", moduleReplacementName);
            return requireImpl(moduleReplacementName, getRequireCwd(realm, realm.getEnv()), realm, activeScriptOrModule);
        } // no core module replacement alias was found: continue and search in the FS.
        TruffleFile maybeModule;
        try {
            maybeModule = CommonJSResolution.resolve(realm, moduleIdentifier, entryPath);
        } catch (SecurityException | IllegalArgumentException | UnsupportedOperationException e) {
            // Module resolution does not execute JS code. Therefore, an exception at this stage is
            // either IO-related (e.g., file not found) or was raised in a custom Truffle FS.
            // We treat any exception as a module loading failure.
            throw fail(moduleIdentifier, e.getMessage());
        }
        log("module ", moduleIdentifier, " resolved to ", maybeModule);

        if (maybeModule == null) {
            // A custom Truffle FS might still try to map a package specifier to some file.
            TruffleFile maybeCustom = realm.getEnv().getPublicTruffleFile(moduleIdentifier);
            if (maybeCustom.exists()) {
                maybeModule = maybeCustom;
            } else {
                throw fail(moduleIdentifier);
            }
        }
        if (isJsFile(maybeModule) || isCjsFile(maybeModule)) {
            return evalJavaScriptFile(maybeModule, moduleIdentifier, realm, activeScriptOrModule);
        } else if (isJsonFile(maybeModule)) {
            return evalJsonFile(maybeModule, realm);
        } else if (isNodeBinFile(maybeModule)) {
            throw fail(UNSUPPORTED_NODE_FILE, moduleIdentifier);
        } else if (maybeModule.exists() && !isMjsFile(maybeModule)) {
            // No extension matched: still try loading as a CJS file
            return evalJavaScriptFile(maybeModule, moduleIdentifier, realm, activeScriptOrModule);
        } else {
            throw fail(moduleIdentifier);
        }
    }

    private static JSObject evalJavaScriptFile(TruffleFile modulePath, String moduleIdentifier, JSRealm realm, ScriptOrModule activeScriptOrModule) {
        JSContext context = realm.getContext();
        TruffleFile normalizedPath = modulePath.normalize();
        // If cached, return from cache. This is by design to avoid infinite require loops.
        Map<TruffleFile, JSObject> commonJSCache = realm.getCommonJSRequireCache();
        if (commonJSCache.containsKey(normalizedPath)) {
            JSObject moduleBuiltin = commonJSCache.get(normalizedPath);
            log("returning cached ", modulePath);
            return moduleBuiltin;
        }
        // Read the file.
        Source source = sourceFromPath(modulePath.toString(), realm);
        TruffleString filenameBuiltin = Strings.fromJavaString(normalizedPath.toString());
        if (modulePath.getParent() == null && !modulePath.exists()) {
            throw fail(moduleIdentifier);
        }
        // Create `require` and other builtins for this module.
        String dirnameBuiltin = modulePath.getParent() == null ? "." : modulePath.getParent().getAbsoluteFile().normalize().toString();
        JSObject exportsBuiltin = createExportsBuiltin(realm);
        JSObject moduleBuiltin = createModuleBuiltin(realm, exportsBuiltin, false, source);
        addJSModuleProperties(moduleBuiltin, filenameBuiltin);
        JSObject requireBuiltin = createRequireBuiltin(realm, moduleBuiltin, filenameBuiltin);
        JSObject env = createEnvObject(realm, context);
        // Parse the module
        Object moduleExecutableFunction = parseModule(realm, source);
        if (activeScriptOrModule != null) {
            activeScriptOrModule.rememberImportedModuleSource(filenameBuiltin, source);
        }
        // Execute the module.
        if (JSFunction.isJSFunction(moduleExecutableFunction)) {
            log("adding to cache ", normalizedPath);
            commonJSCache.put(normalizedPath, moduleBuiltin);
            try {
                debugStackPush(moduleIdentifier);
                log("executing '", filenameBuiltin, "' for '", moduleIdentifier, "'");
                JSFunction.call(JSArguments.create(moduleExecutableFunction, moduleExecutableFunction, exportsBuiltin, requireBuiltin, moduleBuiltin, filenameBuiltin,
                                Strings.fromJavaString(dirnameBuiltin), env));
                JSObject.set(moduleBuiltin, Strings.LOADED_PROPERTY_NAME, true);
                log("done '", moduleIdentifier, "'");
                return moduleBuiltin;
            } catch (Exception e) {
                log("EXCEPTION: '", moduleIdentifier, "': ", e);
                throw e;
            } finally {
                debugStackPop();
            }
        }
        throw fail(moduleIdentifier);
    }

    private static Object parseModule(JSRealm realm, Source source) {
        CallTarget moduleCallTarget = realm.getEnv().parsePublic(source, MODULE_PARSE_ARGS);
        return moduleCallTarget.call();
    }

    private static JSObject evalJsonFile(TruffleFile jsonFile, JSRealm realm) {
        TruffleFile normalizedPath = jsonFile.normalize();
        // If cached, return from cache. This is by design to avoid infinite require loops.
        Map<TruffleFile, JSObject> commonJSCache = realm.getCommonJSRequireCache();
        if (commonJSCache.containsKey(normalizedPath)) {
            JSObject moduleBuiltin = commonJSCache.get(normalizedPath);
            log("returning cached ", jsonFile);
            return moduleBuiltin;
        }
        try {
            if (fileExists(jsonFile)) {
                Source source;
                TruffleFile file = GlobalBuiltins.resolveRelativeFilePath(jsonFile.toString(), realm.getEnv());
                if (file.isRegularFile()) {
                    source = sourceFromTruffleFile(file);
                } else {
                    throw fail(jsonFile.toString());
                }
                JSFunctionObject parse = (JSFunctionObject) realm.getJsonParseFunctionObject();
                assert source != null;
                TruffleString jsonString = Strings.fromJavaString(source.getCharacters().toString());
                Object jsonObj = JSFunction.call(JSArguments.create(Undefined.instance, parse, jsonString));

                JSObject moduleBuiltin = createModuleBuiltin(realm, jsonObj, true, source);
                log("adding to cache ", normalizedPath);
                commonJSCache.put(normalizedPath, moduleBuiltin);
                return moduleBuiltin;
            }
            throw fail(jsonFile.toString());
        } catch (SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
            throw Errors.createErrorFromException(e);
        }
    }

    static JSException fail(String moduleIdentifier) {
        return JSException.create(JSErrorType.TypeError, "Cannot load module: '" + moduleIdentifier + "'");
    }

    private static JSException fail(String moduleIdentifier, String extraMessage) {
        return JSException.create(JSErrorType.TypeError, "Cannot load module: '" + moduleIdentifier + "': " + extraMessage);
    }

    private static JSObject createModuleBuiltin(JSRealm realm, Object exports, boolean loaded, Source source) {
        JSObject module = JSOrdinary.create(realm.getContext(), realm);
        JSObjectUtil.putDataProperty(module, Strings.EXPORTS_PROPERTY_NAME, exports, JSAttributes.getDefault());
        JSObjectUtil.putDataProperty(module, Strings.LOADED_PROPERTY_NAME, loaded, JSAttributes.getDefault());
        JSObjectUtil.putHiddenProperty(module, MODULE_SOURCE_KEY, source);
        return module;
    }

    private static void addJSModuleProperties(JSObject module, TruffleString filename) {
        JSObjectUtil.putDataProperty(module, Strings.ID_PROPERTY_NAME, filename, JSAttributes.getDefault());
        JSObjectUtil.putDataProperty(module, Strings.FILENAME_PROPERTY_NAME, filename, JSAttributes.getDefault());
    }

    private static JSObject createRequireBuiltin(JSRealm realm, JSObject moduleBuiltin, TruffleString fileNameBuiltin) {
        JSFunctionObject mainRequire = (JSFunctionObject) realm.getCommonJSRequireFunctionObject();
        Object mainResolve = JSObject.get(mainRequire, Strings.RESOLVE_PROPERTY_NAME);
        JSFunctionData functionData = JSFunction.getFunctionData(mainRequire);
        JSObject newRequire = JSFunction.create(realm, functionData);
        JSObjectUtil.putDataProperty(newRequire, Strings.MODULE_PROPERTY_NAME, moduleBuiltin, JSAttributes.getDefault());
        JSObjectUtil.putDataProperty(newRequire, Strings.RESOLVE_PROPERTY_NAME, mainResolve, JSAttributes.getDefault());
        // XXX(db) Here, we store the current filename in the (new) require builtin.
        // In this way, we avoid managing a shadow stack to track the current require's parent.
        // In Node.js, this is done using a (closed) level variable.
        JSObjectUtil.putDataProperty(newRequire, Strings.FILENAME_VAR_NAME, fileNameBuiltin, JSAttributes.getDefault());
        return newRequire;
    }

    private static JSObject createExportsBuiltin(JSRealm realm) {
        return JSOrdinary.create(realm.getContext(), realm);
    }

    private static JSObject createEnvObject(JSRealm realm, JSContext context) {
        JSObject env = JSOrdinary.create(context, realm);
        JSObjectUtil.putDataProperty(env, Strings.ENV_PROPERTY_NAME, JSOrdinary.create(context, realm));
        return env;
    }

    private static boolean isNodeBinFile(TruffleFile maybeModule) {
        return hasExtension(Objects.requireNonNull(maybeModule.getName()), NODE_EXT);
    }

    private static boolean isJsFile(TruffleFile maybeModule) {
        return hasExtension(Objects.requireNonNull(maybeModule.getName()), JS_EXT);
    }

    private static boolean isCjsFile(TruffleFile maybeModule) {
        return hasExtension(Objects.requireNonNull(maybeModule.getName()), CJS_EXT);
    }

    private static boolean isMjsFile(TruffleFile maybeModule) {
        return hasExtension(Objects.requireNonNull(maybeModule.getName()), MJS_EXT);
    }

    private static boolean isJsonFile(TruffleFile maybeModule) {
        return hasExtension(Objects.requireNonNull(maybeModule.getName()), JSON_EXT);
    }

    private static boolean fileExists(TruffleFile modulePath) {
        return modulePath.isRegularFile();
    }

    private static TruffleFile getModuleResolutionEntryPath(JSDynamicObject currentRequire, JSRealm realm) {
        TruffleLanguage.Env env = realm.getEnv();
        if (JSDynamicObject.isJSDynamicObject(currentRequire)) {
            Object maybeFilename = JSObject.get(currentRequire, Strings.FILENAME_VAR_NAME);
            if (maybeFilename instanceof TruffleString str) {
                String fileName = Strings.toJavaString(str);
                if (isFile(env, fileName)) {
                    TruffleFile maybeParent = getParent(env, fileName);
                    if (maybeParent != null) {
                        return maybeParent;
                    }
                }
            }
            // dirname not a string. Use default cwd.
        }
        // This is not a nested `require()` call, so we use the default cwd.
        return getModuleResolveCurrentWorkingDirectory(realm);
    }

    private static TruffleFile getParent(TruffleLanguage.Env env, String fileName) {
        return env.getPublicTruffleFile(fileName).getParent();
    }

    private static boolean isFile(TruffleLanguage.Env env, String fileName) {
        return env.getPublicTruffleFile(fileName).exists();
    }

    private static boolean hasExtension(String fileName, String ext) {
        return fileName.endsWith(ext);
    }

    @Override
    public boolean isCallerSensitive() {
        return true;
    }
}
