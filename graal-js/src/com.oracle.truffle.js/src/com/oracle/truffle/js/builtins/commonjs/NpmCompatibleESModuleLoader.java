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
package com.oracle.truffle.js.builtins.commonjs;

import static com.oracle.truffle.js.builtins.commonjs.CommonJSRequireBuiltin.log;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.CJS_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.FILE;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.JSON_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.JS_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.MJS_EXT;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.NODE_MODULES;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.PACKAGE_JSON;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.getCoreModuleReplacement;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.joinPaths;
import static com.oracle.truffle.js.builtins.commonjs.CommonJSResolution.loadJsonObject;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static com.oracle.truffle.js.runtime.Strings.EXPORTS_PROPERTY_NAME;
import static com.oracle.truffle.js.runtime.Strings.MODULE;
import static com.oracle.truffle.js.runtime.Strings.NAME;
import static com.oracle.truffle.js.runtime.Strings.PACKAGE_JSON_MAIN_PROPERTY_NAME;
import static com.oracle.truffle.js.runtime.Strings.TYPE;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.AbstractModuleRecord;
import com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class NpmCompatibleESModuleLoader extends DefaultESModuleLoader {

    private static final URI TryCommonJS = URI.create("custom:///try-common-js-token");
    private static final URI TryCustomESM = URI.create("custom:///try-custom-esm-token");

    private static final String MODULE_NOT_FOUND = "Module not found: '";
    private static final String UNSUPPORTED_JSON = "JSON packages not supported.";
    private static final String FAILED_BUILTIN = "Failed to load built-in ES module: '";
    private static final String INVALID_MODULE_SPECIFIER = "Invalid module specifier: '";
    private static final String UNSUPPORTED_FILE_EXTENSION = "Unsupported file extension: '";
    private static final String UNSUPPORTED_PACKAGE_EXPORTS = "Unsupported package exports: '";
    private static final String UNSUPPORTED_PACKAGE_IMPORTS = "Unsupported package imports: '";
    private static final String UNSUPPORTED_DIRECTORY_IMPORT = "Unsupported directory import: '";
    private static final String INVALID_PACKAGE_CONFIGURATION = "Invalid package configuration: '";

    public static NpmCompatibleESModuleLoader create(JSRealm realm) {
        return new NpmCompatibleESModuleLoader(realm);
    }

    private NpmCompatibleESModuleLoader(JSRealm realm) {
        super(realm);
    }

    /**
     * Node.js-compatible implementation of ES modules loading.
     *
     * @see <a href="https://nodejs.org/api/esm.html#esm_import_specifiers">ES Modules</a>
     * @see <a href="https://nodejs.org/api/esm.html#esm_resolver_algorithm">Resolver algorithm</a>
     *
     * @param referencingModule Referencing ES Module.
     * @param moduleRequest ES Modules Request.
     * @return ES Module record for this module.
     */
    @TruffleBoundary
    @Override
    public AbstractModuleRecord resolveImportedModule(ScriptOrModule referencingModule, ModuleRequest moduleRequest) {
        String specifier = moduleRequest.specifier().toJavaStringUncached();
        log("IMPORT resolve ", specifier);
        String moduleReplacementName = getCoreModuleReplacement(realm, specifier);
        if (moduleReplacementName != null) {
            return loadCoreModuleReplacement(referencingModule, moduleRequest, moduleReplacementName);
        }
        try {
            TruffleLanguage.Env env = realm.getEnv();
            URI parentURL = getFullPath(referencingModule).toUri();
            URI resolution = esmResolve(specifier, parentURL, env);
            if (resolution == TryCommonJS) {
                // Compatibility mode: try loading as a CommonJS module.
                return tryLoadingAsCommonjsModule(specifier);
            } else {
                if (resolution == TryCustomESM) {
                    // Failed ESM resolution. Give the virtual FS a chance to map to a file.
                    // A custom Truffle FS might still try to map a package specifier to some file.
                    TruffleFile maybeFile = env.getPublicTruffleFile(specifier);
                    if (maybeFile.exists() && !maybeFile.isDirectory()) {
                        return loadModuleFromUrl(referencingModule, moduleRequest, maybeFile, maybeFile.getPath());
                    }
                } else if (resolution != null) {
                    TruffleFile file = env.getPublicTruffleFile(resolution);
                    return loadModuleFromUrl(referencingModule, moduleRequest, file, file.getPath());
                }
            }
            // Really could not load as ESM.
            throw fail(MODULE_NOT_FOUND, specifier);
        } catch (IOException | SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
            log("IMPORT resolve ", specifier, " FAILED ", e.getMessage());
            throw Errors.createErrorFromException(e);
        }
    }

    private AbstractModuleRecord loadCoreModuleReplacement(ScriptOrModule referencingModule, ModuleRequest moduleRequest, String moduleReplacementName) {
        String specifier = moduleRequest.specifier().toJavaStringUncached();
        log("IMPORT resolve built-in ", specifier);
        AbstractModuleRecord existingModule = moduleMap.get(specifier);
        if (existingModule != null) {
            log("IMPORT resolve built-in from cache ", specifier);
            return existingModule;
        }
        Source src;
        if (moduleReplacementName.endsWith(MJS_EXT)) {
            URI maybeUri = asURI(moduleReplacementName);
            if (maybeUri != null) {
                // Load from URI
                TruffleLanguage.Env env = realm.getEnv();
                URI parentURL = getFullPath(referencingModule).toUri();
                URI resolution = esmResolve(moduleReplacementName, parentURL, env);
                assert resolution != null;
                try {
                    TruffleFile file = env.getPublicTruffleFile(resolution);
                    return loadModuleFromUrl(referencingModule, moduleRequest, file, file.getPath());
                } catch (IOException e) {
                    throw fail(FAILED_BUILTIN, specifier);
                }
            } else {
                // Just load the module
                try {
                    String cwdOption = realm.getContextOptions().getRequireCwd();
                    TruffleFile cwd = cwdOption.isEmpty() ? realm.getEnv().getCurrentWorkingDirectory() : realm.getEnv().getPublicTruffleFile(cwdOption);
                    TruffleFile modulePath = joinPaths(cwd, moduleReplacementName);
                    src = Source.newBuilder(ID, modulePath).build();
                } catch (IOException | SecurityException e) {
                    throw fail(FAILED_BUILTIN, specifier);
                }
            }
        } else {
            // Else, try loading as commonjs built-in module replacement
            return tryLoadingAsCommonjsModule(specifier);
        }
        JSModuleData parsedModule = realm.getContext().getEvaluator().envParseModule(realm, src);
        JSModuleRecord record = new JSModuleRecord(parsedModule, this);
        moduleMap.put(specifier, record);
        return record;
    }

    private AbstractModuleRecord tryLoadingAsCommonjsModule(String specifier) {
        AbstractModuleRecord existingModule = moduleMap.get(specifier);
        if (existingModule != null) {
            log("IMPORT resolve built-in from cache ", specifier);
            return existingModule;
        }
        JSFunctionObject require = (JSFunctionObject) realm.getCommonJSRequireFunctionObject();
        // Any exception thrown during module loading will be propagated
        Object maybeModule = JSFunction.call(JSArguments.create(Undefined.instance, require, Strings.fromJavaString(specifier)));
        if (maybeModule == Undefined.instance || !JSDynamicObject.isJSDynamicObject(maybeModule)) {
            throw fail(FAILED_BUILTIN, specifier);
        }
        JSDynamicObject module = (JSDynamicObject) maybeModule;
        // Wrap any exported symbol in an ES module.
        List<TruffleString> exportedValues = JSObject.enumerableOwnNames(module);
        var moduleBody = Strings.builderCreate();
        Strings.builderAppend(moduleBody, "const builtinModule = require('");
        Strings.builderAppend(moduleBody, specifier);
        Strings.builderAppend(moduleBody, "');\n");
        for (TruffleString s : exportedValues) {
            Strings.builderAppend(moduleBody, "export const ");
            Strings.builderAppend(moduleBody, s);
            Strings.builderAppend(moduleBody, " = builtinModule.");
            Strings.builderAppend(moduleBody, s);
            Strings.builderAppend(moduleBody, ";\n");
        }
        Strings.builderAppend(moduleBody, "export default builtinModule;");
        Source src = Source.newBuilder(ID, Strings.builderToJavaString(moduleBody), specifier + "-internal.mjs").build();
        JSModuleData parsedModule = realm.getContext().getEvaluator().envParseModule(realm, src);
        JSModuleRecord record = new JSModuleRecord(parsedModule, this);
        moduleMap.put(specifier, record);
        return record;
    }

    //
    // #### ESM resolution algorithm emulation.
    //
    // Best-effort implementation based on Node.js' v16.15.0 resolution algorithm.
    //

    /**
     * ESM_RESOLVE(specifier, parentURL).
     */
    private URI esmResolve(String specifier, URI parentURL, TruffleLanguage.Env env) {
        // 1. Let resolved be undefined.
        URI resolved = asURI(specifier);
        // 2. If specifier is a valid URL, then
        // 2.1 Set resolved to the result of parsing and reserializing specifier as a URL.
        if (resolved == null) {
            if (!specifier.isEmpty() && specifier.charAt(0) == '/' || isRelativePathFileName(specifier)) {
                // 3. Otherwise, if specifier starts with "/", "./" or "../", then
                // 3.1 Set resolved to the URL resolution of specifier relative to parentURL.
                resolved = resolveRelativeToParent(specifier, parentURL);
            } else if (!specifier.isEmpty() && specifier.charAt(0) == '#') {
                // 4. Otherwise, if specifier starts with "#", then
                throw fail(UNSUPPORTED_PACKAGE_IMPORTS, specifier);
            } else {
                // 5.1 Note: specifier is now a bare specifier.
                // 5.2 Set resolvedURL the result of PACKAGE_RESOLVE(specifier, parentURL).
                resolved = packageResolve(specifier, parentURL, env);
            }
        }
        if (resolved == null) {
            // package was not found: will try loading as CommonJS module instead
            return TryCommonJS;
        } else if (resolved == TryCommonJS || resolved == TryCustomESM) {
            // Try customFS lookup
            return resolved;
        }
        // 6. Let format be undefined.
        Format format;
        // 7. If resolved is a "file:" URL, then
        if (isFileURI(resolved)) {
            // 7.1 If resolvedURL contains any percent encodings of "/" or "\" ("%2f" and "%5C"
            // respectively), then
            if (resolved.toString().toUpperCase().contains("%2F") || resolved.toString().toUpperCase().contains("%5C")) {
                // 7.1.1 Throw an Invalid Module Specifier error.
                throw fail(INVALID_MODULE_SPECIFIER, specifier);
            }
            // 7.2 If the file at resolved is a directory, then
            if (isDirectory(resolved, env)) {
                // 7.2.1 Throw an Unsupported Directory Import error.
                throw fail(UNSUPPORTED_DIRECTORY_IMPORT, specifier);
            }
            // 7.3 If the file at resolved does not exist, then
            if (!fileExists(resolved, env)) {
                // 7.3.1 Throw a Module Not Found error.
                throw fail(MODULE_NOT_FOUND, specifier);
            }
            // 7.4 Set resolved to the real path of resolved, maintaining the same URL querystring
            // and fragment components.
            resolved = resolved.normalize();
            // 7.5 Set format to the result of ESM_FILE_FORMAT(resolved).
            format = esmFileFormat(resolved, env);
        } else {
            // 8. Otherwise
            // 8.1 Set format the module format of the content type associated with the URL
            // resolved.
            format = getAssociatedDefaultFormat(resolved);
        }
        if (format == Format.CommonJS) {
            // Will load as CommonJS.
            return TryCommonJS;
        } else {
            // Will load as ESM.
            return resolved;
        }
    }

    /**
     * ESM_FILE_FORMAT(url).
     */
    private Format esmFileFormat(URI url, TruffleLanguage.Env env) {
        // 1. Assert: url corresponds to an existing file.
        assert fileExists(url, env);
        // 2. If url ends in ".mjs", Return "module".
        if (url.getPath().endsWith(MJS_EXT)) {
            return Format.ESM;
        }
        // 3. If url ends in ".cjs", Return "module".
        if (url.getPath().endsWith(CJS_EXT)) {
            // Note: we will try loading as CJS like Node.js does.
            return Format.CommonJS;
        }
        // 4. If url ends in ".mjs", Return "module".
        if (url.getPath().endsWith(JSON_EXT)) {
            throw failMessage(UNSUPPORTED_JSON);
        }
        // 5. Let packageURL be the result of LOOKUP_PACKAGE_SCOPE(url).
        URI packageUri = lookupPackageScope(url, env);
        if (packageUri != null) {
            // 6. Let pjson be the result of READ_PACKAGE_JSON(packageURL).
            PackageJson pjson = readPackageJson(packageUri, env);
            // 7. If pjson?.type exists and is "module", then
            if (pjson != null && pjson.hasTypeModule()) {
                // 7.1 If url ends in ".js", then Return "module"
                if (url.getPath().endsWith(JS_EXT)) {
                    return Format.ESM;
                }
            }
        } else if (url.getPath().endsWith(JS_EXT)) {
            // Np Package.json with .js extension: try loading as CJS like Node.js does.
            return Format.CommonJS;
        }
        // 8. Otherwise, Throw an Unsupported File Extension error.
        throw fail(UNSUPPORTED_FILE_EXTENSION, url.toString());
    }

    /**
     * PACKAGE_RESOLVE(packageSpecifier, parentURL).
     */
    private URI packageResolve(String packageSpecifier, URI parentURL, TruffleLanguage.Env env) {
        // 1. Let packageName be undefined.
        String packageName;
        // 2. If packageSpecifier is an empty string, then
        if (packageSpecifier.isEmpty()) {
            // Throw an Invalid Module Specifier error.
            throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier);
        }
        // 3. Note: we ignore Node.js builtin module names.
        int packageSpecifierSeparator = packageSpecifier.indexOf('/');
        // 4. If packageSpecifier does not start with "@", then
        if (packageSpecifier.charAt(0) != '@') {
            // Set packageName to the substring of packageSpecifier until the first "/"
            if (packageSpecifierSeparator != -1) {
                packageName = packageSpecifier.substring(0, packageSpecifierSeparator);
            } else {
                // or the end of the string.
                packageName = packageSpecifier;
            }
        } else {
            // 5. Otherwise, if packageSpecifier does not contain a "/" separator, then
            if (packageSpecifierSeparator == -1) {
                // Throw an Invalid Module Specifier error.
                throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier);
            }
            // Set packageName to the substring of packageSpecifier until the second "/" separator
            int secondSeparator = packageSpecifier.indexOf('/', packageSpecifierSeparator + 1);
            if (secondSeparator != -1) {
                packageName = packageSpecifier.substring(0, secondSeparator);
            } else {
                // or the end of the string.
                packageName = packageSpecifier;
            }
        }
        // 6. If packageName starts with "." or contains "\" or "%", then
        if (packageName.charAt(0) == '.' || packageName.indexOf('\\') >= 0 || packageName.indexOf('%') >= 0) {
            // Throw an Invalid Module Specifier error.
            throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier);
        }
        // 7. Let packageSubpath be "." concatenated with the substring of packageSpecifier from the
        // position at the length of packageName.
        String packageSpecifierSub = packageSpecifier.substring(packageName.length());
        String packageSubpath = DOT + packageSpecifierSub;
        // 8. If packageSubpath ends in "/", then
        if (packageSubpath.endsWith(SLASH)) {
            // Throw an Invalid Module Specifier error.
            throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier);
        }
        // 9. Let selfUrl be the result of PACKAGE_SELF_RESOLVE(packageName, packageSubpath,
        // parentURL).
        URI selfUrl = packageSelfResolve(packageName, parentURL, env);
        // 10. If selfUrl is not undefined, return selfUrl.
        if (selfUrl != null) {
            return selfUrl;
        }
        TruffleFile currentParentUrl = env.getPublicTruffleFile(parentURL);
        // 11. While parentURL is not the file system root,
        while (currentParentUrl != null && !isRoot(currentParentUrl)) {
            // 11.1 Let packageURL be the URL resolution of "node_modules/" concatenated with
            // packageSpecifier, relative to parentURL.
            URI packageUrl = getPackageUrl(packageName, currentParentUrl);
            // 11.2 Set parentURL to the parent folder URL of parentURL.
            currentParentUrl = currentParentUrl.getParent();
            // 11.3 If the folder at packageURL does not exist, then
            TruffleFile maybeFolder = packageUrl != null ? env.getPublicTruffleFile(packageUrl) : null;
            if (maybeFolder == null || !maybeFolder.exists() || !maybeFolder.isDirectory()) {
                continue;
            }
            // 11.4 Let pjson be the result of READ_PACKAGE_JSON(packageURL).
            PackageJson pjson = readPackageJson(packageUrl, env);
            // 11.5 If pjson is not null and pjson.exports is not null or undefined, then
            if (pjson != null && pjson.hasExportsProperty()) {
                throw fail(UNSUPPORTED_PACKAGE_EXPORTS, packageSpecifier);
            } else if (packageSubpath.equals(DOT)) {
                // 11.6 Otherwise, if packageSubpath is equal to ".", then
                // 11.6.1 If pjson.main is a string, then return the URL resolution of main in
                // packageURL.
                if (pjson != null && pjson.hasMainProperty()) {
                    TruffleString main = pjson.getMainProperty();
                    return packageUrl.resolve(main.toString());
                } else {
                    // For backwards compatibility: return null and try loading as a legacy CJS.
                    // https://github.com/oracle/graaljs/blob/master/graal-nodejs/lib/internal/modules/esm/resolve.js#L918
                    return TryCommonJS;
                }
            }
            // 7. Otherwise, Return the URL resolution of packageSubpath in packageURL.
            return packageUrl.resolve(packageSubpath);
        }
        // 12. Will Throw a Module Not Found error.
        return TryCustomESM;
    }

    private static boolean isRoot(TruffleFile file) {
        if (file.isDirectory() && file.isAbsolute()) {
            return file.getParent() == null;
        }
        return false;
    }

    /**
     * PACKAGE_SELF_RESOLVE(packageName, packageSubpath, parentURL).
     */
    private URI packageSelfResolve(String packageName, URI parentURL, TruffleLanguage.Env env) {
        // 1. Let packageURL be the result of LOOKUP_PACKAGE_SCOPE(parentURL).
        URI packageUrl = lookupPackageScope(parentURL, env);
        // 2. If packageURL is null, then Return undefined.
        if (packageUrl == null) {
            return null;
        }
        // 3. Let pjson be the result of READ_PACKAGE_JSON(packageURL).
        PackageJson pjson = readPackageJson(packageUrl, env);
        // 4. If pjson is null or if pjson.exports is null or undefined, Return undefined.
        if (pjson == null || !pjson.hasExportsProperty()) {
            return null;
        }
        // 5. If pjson.name is equal to packageName, then
        if (pjson.namePropertyEquals(packageName)) {
            throw failMessage(UNSUPPORTED_PACKAGE_EXPORTS);
        }
        // 6. Otherwise, return undefined.
        return null;
    }

    /**
     * LOOKUP_PACKAGE_SCOPE(url).
     */
    private URI lookupPackageScope(URI url, TruffleLanguage.Env env) {
        // 1. Let scopeURL be url
        URI scopeUrl = url;
        // 2. While scopeURL is not the file system root,
        while (scopeUrl != null) {
            // 2.1. Set scopeURL to the parent URL of scopeURL.
            scopeUrl = getParentUrl(scopeUrl, env);
            if (scopeUrl == null) {
                break;
            }
            // 2.2 If scopeURL ends in a "node_modules" path segment, return null.
            if (scopeUrl.toString().endsWith(NODE_MODULES)) {
                return null;
            }
            // 2.3 Let pjsonURL be the resolution of "package.json" within scopeURL.
            // 2.4 if the file at pjsonURL exists, then Return scopeURL
            if (readPackageJson(scopeUrl, env) != null) {
                return scopeUrl;
            }
        }
        // 3. Return null.
        return null;
    }

    //
    // ##### Utils
    //

    private enum Format {
        CommonJS,
        ESM
    }

    private static class PackageJson {

        private final JSDynamicObject jsonObj;

        PackageJson(JSDynamicObject jsonObj) {
            assert jsonObj != null;
            assert JSObject.isJSObject(jsonObj);
            this.jsonObj = jsonObj;
        }

        boolean hasTypeModule() {
            if (hasNonNullProperty(jsonObj, TYPE)) {
                Object nameValue = JSObject.get(jsonObj, TYPE);
                if (nameValue instanceof TruffleString nameStr) {
                    return Strings.equals(MODULE, nameStr);
                }
            }
            return false;
        }

        private static boolean hasNonNullProperty(JSDynamicObject object, TruffleString keyName) {
            if (JSObject.hasProperty(object, keyName)) {
                Object value = JSObject.get(object, keyName);
                return value != Null.instance && value != Undefined.instance;
            }
            return false;
        }

        public boolean hasExportsProperty() {
            return hasNonNullProperty(jsonObj, EXPORTS_PROPERTY_NAME);
        }

        public boolean hasMainProperty() {
            if (JSObject.hasProperty(jsonObj, PACKAGE_JSON_MAIN_PROPERTY_NAME)) {
                Object value = JSObject.get(jsonObj, PACKAGE_JSON_MAIN_PROPERTY_NAME);
                return Strings.isTString(value);
            }
            return false;
        }

        public TruffleString getMainProperty() {
            assert hasMainProperty();
            Object value = JSObject.get(jsonObj, PACKAGE_JSON_MAIN_PROPERTY_NAME);
            return (TruffleString) value;
        }

        public boolean namePropertyEquals(String name) {
            TruffleString packageName = Strings.fromJavaString(name);
            if (hasNonNullProperty(jsonObj, NAME)) {
                Object nameValue = JSObject.get(jsonObj, NAME);
                if (nameValue instanceof TruffleString nameStr) {
                    return Strings.equals(packageName, nameStr);
                }
            }
            return false;
        }
    }

    private PackageJson readPackageJson(URI packageUrl, TruffleLanguage.Env env) {
        URI pjsonUrl = packageUrl.resolve(PACKAGE_JSON);
        if (!fileExists(pjsonUrl, env)) {
            return null;
        }
        JSDynamicObject jsonObj = loadJsonObject(env.getPublicTruffleFile(pjsonUrl), realm);
        if (!JSDynamicObject.isJSDynamicObject(jsonObj)) {
            throw failMessage(INVALID_PACKAGE_CONFIGURATION);
        }
        return new PackageJson(jsonObj);
    }

    private static boolean fileExists(URI url, TruffleLanguage.Env env) {
        return CommonJSResolution.fileExists(env.getPublicTruffleFile(url));
    }

    private static boolean isFileURI(URI maybe) {
        return maybe != null && maybe.getScheme().equals(FILE);
    }

    private static URI getPackageUrl(String packageSpecifier, TruffleFile parentURL) {
        try {
            URI combined = new URI("./" + NODE_MODULES + "/" + packageSpecifier);
            TruffleFile resolved = parentURL.resolve(String.valueOf(combined));
            return resolved.toUri();
        } catch (URISyntaxException e) {
            // will handle null return
        }
        return null;
    }

    private static URI getParentUrl(URI scopeUrl, TruffleLanguage.Env env) {
        TruffleFile asFile = env.getPublicTruffleFile(scopeUrl);
        if (asFile.getParent() != null) {
            return asFile.getParent().toUri();
        }
        return null;
    }

    private static Format getAssociatedDefaultFormat(URI resolved) {
        assert resolved.getPath() != null;
        if (resolved.getPath().endsWith(MJS_EXT)) {
            return Format.ESM;
        }
        // By default, try loading as CJS if not .mjs
        return Format.CommonJS;
    }

    private static boolean isDirectory(URI resolved, TruffleLanguage.Env env) {
        return env.getPublicTruffleFile(resolved).isDirectory();
    }

    private static URI resolveRelativeToParent(String specifier, URI parentURL) {
        return parentURL.resolve(specifier);
    }

    private TruffleFile getFullPath(ScriptOrModule referencingModule) {
        String refPath = referencingModule == null ? null : referencingModule.getSource().getPath();
        if (refPath == null) {
            refPath = realm.getContextOptions().getRequireCwd();
        }
        return realm.getEnv().getPublicTruffleFile(refPath);
    }

    @TruffleBoundary
    private static JSException failMessage(String message) {
        return JSException.create(JSErrorType.TypeError, message);
    }

    @TruffleBoundary
    private static JSException fail(String errorType, String moduleIdentifier) {
        return failMessage(errorType + moduleIdentifier + Strings.SINGLE_QUOTE);
    }

    private static boolean isRelativePathFileName(String moduleIdentifier) {
        return moduleIdentifier.startsWith(DOT_SLASH) || moduleIdentifier.startsWith(DOT_DOT_SLASH);
    }
}
