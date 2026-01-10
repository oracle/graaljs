/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.Strings.IMPORTS_PROPERTY_NAME;
import static com.oracle.truffle.js.runtime.Strings.NAME;
import static com.oracle.truffle.js.runtime.Strings.PACKAGE_JSON_MAIN_PROPERTY_NAME;
import static com.oracle.truffle.js.runtime.Strings.TYPE;
import static com.oracle.truffle.js.runtime.Strings.constant;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import com.oracle.truffle.js.runtime.array.ScriptArray;
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

    private static final String TYPE_MODULE = "module";
    private static final String TYPE_COMMONS_JS = "commonjs";
    private static final String MODULE_NOT_FOUND = "Module not found: '";
    private static final String UNSUPPORTED_JSON = "JSON packages not supported.";
    private static final String FAILED_BUILTIN = "Failed to load built-in ES module: '";
    private static final String INVALID_MODULE_SPECIFIER = "Invalid module specifier: '";
    private static final String UNSUPPORTED_FILE_EXTENSION = "Unsupported file extension: '";
    private static final String INVALID_PACKAGE_TARGET = "Invalid package export: '";
    private static final String PACKAGE_PATH_NOT_EXPORTED = "Package subpath is not defined by \"exports\" field: '";
    private static final String PACKAGE_IMPORT_NOT_DEFINED = "Packages imports do not define the specifier: '";
    private static final String UNSUPPORTED_DIRECTORY_IMPORT = "Unsupported directory import: '";
    private static final String INVALID_PACKAGE_CONFIGURATION = "Invalid package configuration: '";
    private static final String CONDITION_TYPE_GRAALJS = "graaljs";
    private static final String CONDITION_TYPE_IMPORT = "import";
    private static final String CONDITION_TYPE_REQUIRE = "require";
    private static final String CONDITION_TYPE_DEFAULT = "default";
    private static final char PACKAGE_EXPORT_WILDCARD = '*';
    private static final List<String> DEFAULT_CONDITIONS = List.of(CONDITION_TYPE_GRAALJS, CONDITION_TYPE_IMPORT, CONDITION_TYPE_REQUIRE, CONDITION_TYPE_DEFAULT);

    public List<String> getConditions() {
        return Stream.concat(this.realm.getContextOptions().getUserConditions().stream(),
            DEFAULT_CONDITIONS.stream())
                .toList();
    }

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
            Format format;

            // If resolved is a "file:" URL, then
            if(isFileURI(resolution)){
                // Set format to the result of ESM_FILE_FORMAT(resolved).
                format = esmFileFormat(resolution, env);
            } else {
                // Set format the module format of the content type associated with the URL resolved.
                format = getAssociatedDefaultFormat(resolution);
            }

            if (resolution == TryCustomESM) {
              // Failed ESM resolution. Give the virtual FS a chance to map to a file.
              // A custom Truffle FS might still try to map a package specifier to some file.
              TruffleFile maybeFile = env.getPublicTruffleFile(specifier);
              if (maybeFile.exists() && !maybeFile.isDirectory()) {
                  return loadModuleFromFile(referencingModule, moduleRequest, maybeFile, maybeFile.getPath());
              }
            } else if(isFileURI(resolution) && format == Format.CommonJS) {
                // If esmResolve returns a valid file url and the format is CommonJS,
                //   we will use this path to load the CJS module.
                return tryLoadingAsCommonjsModule(resolution.getRawPath());
            } else if (resolution == TryCommonJS) {
                // Compatibility mode: try loading as a CommonJS module.
                return tryLoadingAsCommonjsModule(specifier);
            } else if (resolution != null) {
                    TruffleFile file = env.getPublicTruffleFile(resolution);
                    return loadModuleFromFile(referencingModule, moduleRequest, file, file.getPath());
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
        CanonicalModuleKey moduleKey = new CanonicalModuleKey(specifier, moduleRequest.attributes());
        AbstractModuleRecord existingModule = moduleMap.get(moduleKey);
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
                    return loadModuleFromFile(referencingModule, moduleRequest, file, file.getPath());
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
        moduleMap.put(moduleKey, record);
        return record;
    }

    private AbstractModuleRecord tryLoadingAsCommonjsModule(String specifier) {
        CanonicalModuleKey moduleKey = new CanonicalModuleKey(specifier, Map.of());
        AbstractModuleRecord existingModule = moduleMap.get(moduleKey);
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
        List<TruffleString> exportedNames = JSObject.enumerableOwnNames(module);
        var moduleBody = new StringBuilder(64);
        moduleBody.append("const builtinModule = require('");
        moduleBody.append(specifier);
        moduleBody.append("');\n");
        for (TruffleString name : exportedNames) {
            moduleBody.append("export const ");
            moduleBody.append(name.toJavaStringUncached());
            moduleBody.append(" = builtinModule.");
            moduleBody.append(name.toJavaStringUncached());
            moduleBody.append(";\n");
        }
        moduleBody.append("export default builtinModule;");
        Source src = Source.newBuilder(ID, moduleBody.toString(), specifier + "-internal.mjs").build();
        JSModuleData parsedModule = realm.getContext().getEvaluator().envParseModule(realm, src);
        JSModuleRecord record = new JSModuleRecord(parsedModule, this);
        moduleMap.put(moduleKey, record);
        return record;
    }

    //
    // #### ESM resolution algorithm emulation.
    //
    // Best-effort implementation based on Node.js' v25.2.1 resolution algorithm.
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
                // 4.1 Set resolved to the result of PACKAGE_IMPORTS_RESOLVE(specifier, parentURL, defaultConditions).
                resolved = packageImportsResolve(specifier, parentURL, getConditions(), env);
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
        }
        return resolved;
    }
    /*
     * PACKAGE_IMPORTS_RESOLVE(specifier, parentURL, conditions)
     */

    private URI packageImportsResolve(String specifier, URI parentURL, List<String> conditions, TruffleLanguage.Env env) {
        // 1. Assert: specifier begins with "#".
        // 2. If specifier is exactly equal to "#" or starts with "#/", then
        if(!specifier.startsWith("#") || specifier.equals("#") || specifier.equals("#/")){
            // 2.1 Throw an Invalid Module Specifier error.
            throw fail(INVALID_MODULE_SPECIFIER, specifier);
        }
        // 3. Let packageURL be the result of LOOKUP_PACKAGE_SCOPE (parentURL).
        var packageURL = lookupPackageScope(parentURL, env);
        // 4. If packageURL is not null, then
        if(packageURL!=null){
            // 4.1 Let pjson be the result of READ_PACKAGE_JSON(packageURL).
            PackageJson pjson = readPackageJson(packageURL, env);
            // 4.2 If pjson.imports is a non-null Object, then
            if(pjson!=null && pjson.hasImportsProperty()){
                JSDynamicObject imports = pjson.getImportsProperty();
                if(imports!=null){
                    // 4.2.1 Let resolved be the result of
                    //   PACKAGE_IMPORTS_EXPORTS_RESOLVE(specifier, pjson.imports, packageURL, true, conditions).
                    URI resolved = packageImportsExportsResolve(specifier, imports, packageURL, true, conditions, env);
                    // 4.2.2 If resolved is not null or undefined, return resolved.
                    if(resolved!=null){
                        return resolved;
                    }
                }
            }
        }
        // 5. Throw a Package Import Not Defined error.
        throw fail(PACKAGE_IMPORT_NOT_DEFINED, specifier);
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
        // - 5. If url ends in ".wasm", then
        // - 6. If --experimental-addon-modules is enabled and url ends in ".node", then
        // 7. Let packageURL be the result of LOOKUP_PACKAGE_SCOPE(url).
        URI packageUri = lookupPackageScope(url, env);
        PackageJson pjson = null;
        if (packageUri != null) {
            // 8. Let pjson be the result of READ_PACKAGE_JSON(packageURL).
             pjson = readPackageJson(packageUri, env);
        }
        // 9. Let packageType be null
        String packageType = null;
        if (pjson != null ) {
            // 10. If pjson?.type is "module" or "commonjs", then
            if(pjson.hasTypeProperty()){
                // 10.1 Set packageType to pjson.type.
                packageType = pjson.getTypeProperty();
            }
        }
        // 11. If url ends in ".js", then
        if (url.getPath().endsWith(JS_EXT)) {
            // 11.1 If packageType is not null, then
            if(packageType!=null){
                // 11.1.1 Return packageType.
                if(packageType.equals(TYPE_MODULE)){
                    return Format.ESM;
                } else {
                    return Format.CommonJS;
                }
            }
            // 11.2 If the result of DETECT_MODULE_SYNTAX(source) is true, then
            // not implemented
            // 11.3 Return "commonjs"
            return Format.CommonJS;
        }
        // 12. If url does not have any extension, then
        //  not implemented
        throw fail(UNSUPPORTED_FILE_EXTENSION, url.toString());
    }

    /**
     * PACKAGE_EXPORTS_RESOLVE(packageURL, subpath, exports, conditions)
     */
    private URI packageExportsResolve(URI packageURL, String subpath, Object exports, List<String> conditions, TruffleLanguage.Env env){
        URI resolved = null;
        // 1. If exports is an Object with both a key starting with "." and a key not starting with ".",
        //   throw an Invalid Package Configuration error.
        boolean hasStartingWithDot = false;
        if(exports instanceof JSDynamicObject exportsObj){
            List<TruffleString> keys = JSObject.enumerableOwnNames(exportsObj);
            for(int i = 0; i < keys.size(); i++){
                var keyTStr = keys.get(i);
                var keyStr = keyTStr.toString();
                boolean startingWithDot = keyStr.startsWith(".");
                // If exports is an Object with both a key starting with "." and a key not starting with ".", throw an Invalid Package Configuration error.
                if(i != 0 && hasStartingWithDot != startingWithDot){
                    throw fail(INVALID_PACKAGE_CONFIGURATION, packageURL.toString());
                }
                hasStartingWithDot = startingWithDot;
            }
        }
        // 2. If subpath is equal to ".", then
        if(subpath.equals(".")){
            // 2.1 Let mainExport be undefined
            Object mainExport = null;
            // 2.2 If exports is a String or Array,
            //       or an Object containing no keys starting with ".", then
            if(exports instanceof TruffleString || (exports instanceof JSDynamicObject && !hasStartingWithDot)
               || JSObject.hasArray(exports)){
                mainExport = exports;
                // 2.3 Otherwise if exports is an Object containing a "." property, then
            } else if(exports instanceof JSDynamicObject exportsObj &&
                      exportsObj.hasOwnProperty(constant(DOT))){
                // 2.3.1 Set mainExport to exports["."].
                mainExport = JSObject.get(exportsObj, constant(DOT));
            }
            // 2.4 If mainExport is not undefined, then
            if(mainExport != null){
                // 2.4.1 Let resolved be the result of PACKAGE_TARGET_RESOLVE(packageURL, mainExport, null, false, conditions).
                resolved = packageTargetResolve(packageURL, mainExport, null, false, conditions, env);
                // 2.4.2 If resolved is not null or undefined, return resolved.
                if(resolved!=null){
                    return resolved;
                }
            }
        } else {
            // 3. Otherwise, if exports is an Object and all keys of exports start with ".", then
            if(exports instanceof JSDynamicObject exportsObj && hasStartingWithDot){
                // 3.1 Assert: subpath begins with "./".
                if(!subpath.startsWith("./")){
                    throw fail(INVALID_MODULE_SPECIFIER, subpath);
                }
                // 3.2 Let resolved be the result of PACKAGE_IMPORTS_EXPORTS_RESOLVE( subpath, exports, packageURL, false, conditions).
                resolved = packageImportsExportsResolve(subpath, exportsObj, packageURL, false, conditions, env);
                // 3.3 If resolved is not null or undefined, return resolved.
                if(resolved!=null){
                    return resolved;
                }
            }
        }
        // 4. Throw a Package Path Not Exported error.
        throw fail(PACKAGE_PATH_NOT_EXPORTED, subpath);
    }

    private static int countChar(String s, char c){
        return s.length() - s.replace(String.valueOf(c), "").length();
    }

    /**
     * PATTERN_KEY_COMPARE(keyA, keyB)
     */
    private static int patternKeyCompare(String keyA, String keyB, URI packageURL){
        // 1. Assert: keyA contains only a single "*".
        // 2. Assert: keyB contains only a single "*".
        if(countChar(keyA, PACKAGE_EXPORT_WILDCARD)!=1 || countChar(keyB, PACKAGE_EXPORT_WILDCARD)!=1){
            throw fail(INVALID_PACKAGE_TARGET, packageURL.toString());
        }
        // 3. Let baseLengthA be the index of "*" in keyA.
        var baseLengthA = keyA.indexOf(PACKAGE_EXPORT_WILDCARD);
        // 4. Let baseLengthB be the index of "*" in keyB.
        var baseLengthB = keyB.indexOf(PACKAGE_EXPORT_WILDCARD);
        // 5. If baseLengthA is greater than baseLengthB, return -1.
        if(baseLengthA > baseLengthB){
            return -1;
        }
        // 6. If baseLengthB is greater than baseLengthA, return 1.
        if(baseLengthB > baseLengthA) {
            return 1;
        }
        // 7. If the length of keyA is greater than the length of keyB, return -1.
        if(keyA.length() > keyB.length()){
            return -1;
        }
        // 8. If the length of keyB is greater than the length of keyA, return 1.
        if(keyB.length() > keyA.length()){
            return 1;
        }
        // 9. Return 0.
        return 0;
    }

    /**
     * PACKAGE_IMPORTS_EXPORTS_RESOLVE(matchKey, matchObj, packageURL, isImports, conditions)
     */
    private URI packageImportsExportsResolve(String matchKey, JSDynamicObject matchObj, URI packageURL, boolean isImports, List<String> conditions, TruffleLanguage.Env env) {
        // 1. If matchKey ends in "/", then
        if (matchKey.endsWith("/")) {
            // 1.1 Throw an Invalid Module Specifier error.
            throw fail(INVALID_MODULE_SPECIFIER, matchKey);
        }
        // 2.If matchKey is a key of matchObj and does not contain "*", then
        if (!matchKey.contains("*") && matchObj.hasOwnProperty(constant(matchKey))) {
            // 2.1 Let target be the value of matchObj[matchKey].
            var target = JSObject.get(matchObj, constant(matchKey));
            // 2.2 Return the result of PACKAGE_TARGET_RESOLVE(packageURL, target, null, isImports, conditions).
            return packageTargetResolve(packageURL, target, null, isImports, conditions, env);
        }
        var expansionKeys = JSObject.enumerableOwnNames(matchObj).stream().map(key -> key.toString())
                        // 3. Let expansionKeys be the list of keys of matchObj containing only a
                        //  single "*"
                        .filter(key -> countChar(key, PACKAGE_EXPORT_WILDCARD) == 1)
                        // 3. sorted by the sorting function PATTERN_KEY_COMPARE which orders in
                        //    descending order of specificity
                        .sorted((keyA, keyB) -> patternKeyCompare(keyA, keyB, packageURL)).toList();
        for (var expansionKey : expansionKeys) {
            // 4. For each key expansionKey in expansionKeys, do
            // 4.1 Let patternBase be the substring of expansionKey up to but excluding the first
            //   "*" character.
            var patternBase = expansionKey.substring(0, expansionKey.indexOf(PACKAGE_EXPORT_WILDCARD));
            // 4.2 If matchKey starts with but is not equal to patternBase, then
            if (!matchKey.equals(patternBase) && matchKey.startsWith(patternBase)) {
                // 4.2.1 Let patternTrailer be the substring of expansionKey from the index after
                //   the first "*" character.
                var patternTrailer = expansionKey.substring(expansionKey.indexOf(PACKAGE_EXPORT_WILDCARD) + 1);
                // 4.2.2 If patternTrailer has zero length, or if matchKey ends with patternTrailer
                //   and the length of matchKey is greater than or equal to the length of
                //   expansionKey, then
                if (patternTrailer.isEmpty() || (matchKey.endsWith(patternTrailer) && matchKey.length() >= expansionKey.length())) {
                    // 4.2.2.1 Let target be the value of matchObj[expansionKey].
                    var target = JSObject.get(matchObj, constant(expansionKey));
                    // 4.2.2.2 Let patternMatch be the substring of matchKey
                    //   starting at the index of the length of patternBase up to
                    //   the length of matchKey minus the length of patternTrailer.
                    var patternMatch = matchKey.substring(patternBase.length(), matchKey.length() - patternTrailer.length());
                    // 4.2.2.3 Return the result of
                    //   PACKAGE_TARGET_RESOLVE(packageURL, target, patternMatch, isImports,
                    //   conditions).
                    return packageTargetResolve(packageURL, target, patternMatch, isImports, conditions, env);
                }
            }
        }
        // 5. Return null.
        return null;
    }

    /**
     * PACKAGE_TARGET_RESOLVE(packageURL, target, patternMatch, isImports, conditions)
     */
    private URI packageTargetResolve(URI packageURL, Object target, String patternMatch, boolean isImports, List<String> conditions, TruffleLanguage.Env env) {
        // 1. If target is a String, then
        if (target instanceof TruffleString targetTStr) {
            String targetStr = targetTStr.toString();
            // 1.1 If target does not start with "./", then
            if (!targetStr.startsWith("./")) {
                boolean isValidUrl = (asURI(targetStr) != null);
                // 1.1.1 If isImports is false, or if target starts with "../" or "/", or if target
                // is a valid URL, then
                if (!isImports || targetStr.startsWith("../") || targetStr.startsWith("/") || isValidUrl) {
                    throw fail(INVALID_PACKAGE_TARGET, targetStr);
                }
                // 1.1.2 If patternMatch is a String, then
                if (patternMatch != null) {
                    // 1.1.2.1 Return PACKAGE_RESOLVE(target with every instance of "*" replaced by
                    // patternMatch, packageURL + "/").
                    return packageResolve(targetStr.replaceAll(Pattern.quote(String.valueOf(PACKAGE_EXPORT_WILDCARD)), patternMatch),
                                    packageURL, env);
                } else {
                    // 1.1.3 Return PACKAGE_RESOLVE(target, packageURL + "/").
                    return packageResolve(targetStr, packageURL, env);
                }
            } else {
                // 1.2 If target split on "/" or "\" contains any "", ".", "..", or "node_modules"
                // segments after the first "." segment, case insensitive and including percent
                // encoded variants,
                for (String seg : targetStr.substring(2).split("[/|\\\\]")) {
                    if (seg.isEmpty() || seg.equals(DOT) || seg.equals(DOT + DOT) || seg.equalsIgnoreCase(NODE_MODULES)) {
                        // throw an Invalid Package Target error.
                        throw fail(INVALID_PACKAGE_TARGET, targetStr);
                    }
                }
                // 1.3 Let resolvedTarget be the URL resolution of the concatenation of packageURL
                // and target.
                var resolvedTarget = resolveRelativeToParent(targetStr, packageURL);
                // 1.4 Assert: packageURL is contained in resolvedTarget.
                if (!resolvedTarget.normalize().getPath().startsWith(packageURL.normalize().getPath())) {
                    throw fail(INVALID_PACKAGE_TARGET, targetStr);
                }
                // 1.5 If patternMatch is null, then
                if (patternMatch == null) {
                    // 1.5.1 Return resolvedTarget.
                    return resolvedTarget;
                }
                // 1.6 If patternMatch split on "/" or "\" contains any "", ".", "..", or
                // "node_modules" segments, case insensitive and including percent encoded variants.
                for (String seg : patternMatch.split("[/|\\\\]")) {
                    if (seg.isEmpty() || seg.equals(DOT) || seg.equals(DOT + DOT) || seg.equalsIgnoreCase(NODE_MODULES)) {
                        // throw an Invalid Module Specifier error.
                        throw fail(INVALID_MODULE_SPECIFIER, patternMatch);
                    }
                }
                // 1.7 Return the URL resolution of resolvedTarget with every instance of "*"
                // replaced with patternMatch.
                return asURI(resolvedTarget.toString().replaceAll(Pattern.quote(String.valueOf(PACKAGE_EXPORT_WILDCARD)), patternMatch));
            }
        } else if (target instanceof JSDynamicObject targetObj && !JSObject.hasArray(targetObj)) {
            // 2 Otherwise, if target is a non-null Object, then

            // 2.1 If target contains any index property keys, as defined in ECMA-262 6.1.7 Array
            // Index, throw an Invalid Package Configuration error.
            for (var key : targetObj.ownPropertyKeys()) {
                if (!(key instanceof TruffleString)) {
                    throw fail(INVALID_PACKAGE_CONFIGURATION, targetObj.toString());
                }
            }

			// 2.2 For each property p of target, in object insertion order as
			for (var keyTStr : JSObject.enumerableOwnNames(targetObj)) {
				var p = keyTStr.toString();
				// 2.2.1 If p equals "default" or conditions contains an entry for p, then
				if (p.equals("default") || conditions.contains(p)) {
					// 2.2.1 Let targetValue be the value of the p property in target.
					var targetValue = JSObject.get(targetObj, keyTStr);
					// 2.2.2 Let resolved be the result of
					// PACKAGE_TARGET_RESOLVE(packageURL, targetValue, patternMatch, isImports,
					// conditions).
					var resolved = packageTargetResolve(packageURL, targetValue, patternMatch, isImports, conditions, env);
					// 2.2.3 If resolved is equal to undefined, continue the loop
					if (resolved != null) {
						// 2.2.4 Return resolved
						return resolved;
					}
				}
			}
			// 2.3 Return undefined.
			return null;
        } else if (target instanceof JSDynamicObject targetObj && JSObject.hasArray(targetObj)) {
            // 3. Otherwise, if target is an Array, then
            ScriptArray _target = JSObject.getArray(targetObj);
            // 3.1 If _target.length is zero, return null.
            if (_target.length(targetObj) == 0) {
                return null;
            }
            // 3.2 For each item targetValue in target, do
            for (int i = 0; i < _target.length(targetObj); i++) {
                var targetValue = _target.getElement(targetObj, i);
                // 3.2.1 Let resolved be the result of PACKAGE_TARGET_RESOLVE( packageURL,
                // targetValue, patternMatch, isImports, conditions), continuing the loop on any
                // Invalid Package Target error.
                var resolved = packageTargetResolve(packageURL, targetValue, patternMatch, isImports, conditions, env);
                // 3.2.2 If resolved is undefined, continue the loop.
                // 3.2.3 Return resolved.
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        // 4. Otherwise, if target is null, return null.
        if (target == null) {
            return null;
        }
        // 5. Otherwise throw an Invalid Package Target error.
        throw fail(INVALID_PACKAGE_TARGET, target.toString());
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

        if (packageSubpath.endsWith(SLASH)) {
            // Throw an Invalid Module Specifier error.
            throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier);
        }

        // 8. Let selfUrl be the result of PACKAGE_SELF_RESOLVE(packageName, packageSubpath,
        // parentURL).
        URI selfUrl = packageSelfResolve(packageName, packageSubpath, parentURL, env);
        // 9. If selfUrl is not undefined, return selfUrl.
        if (selfUrl != null) {
            return selfUrl;
        }
        TruffleFile currentParentUrl = env.getPublicTruffleFile(parentURL);
        // 10. While parentURL is not the file system root,
        while (currentParentUrl != null && !isRoot(currentParentUrl)) {
            // 10.1 Let packageURL be the URL resolution of "node_modules/" concatenated with
            // packageSpecifier, relative to parentURL.
            URI packageUrl = getPackageUrl(packageName, currentParentUrl);
            // 10.2 Set parentURL to the parent folder URL of parentURL.
            currentParentUrl = currentParentUrl.getParent();
            // 10.3 If the folder at packageURL does not exist, then
            TruffleFile maybeFolder = packageUrl != null ? env.getPublicTruffleFile(packageUrl) : null;
            if (maybeFolder == null || !maybeFolder.exists() || !maybeFolder.isDirectory()) {
                continue;
            }
            // 10.4 Let pjson be the result of READ_PACKAGE_JSON(packageURL).
            PackageJson pjson = readPackageJson(packageUrl, env);
            // 10.5 If pjson is not null and pjson.exports is not null or undefined, then
            if (pjson != null && pjson.hasExportsProperty()) {
                // 10.5.1 Return the result of PACKAGE_EXPORTS_RESOLVE(packageURL, packageSubpath, pjson.exports, defaultConditions).
                return packageExportsResolve(packageUrl, packageSubpath, pjson.getExportsProperty(), getConditions(), env);
            } else if (packageSubpath.equals(DOT)) {
                // 10.6 Otherwise, if packageSubpath is equal to ".", then
                // 10.6.1 If pjson.main is a string, then return the URL resolution of main in
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
            // 10.7. Otherwise, Return the URL resolution of packageSubpath in packageURL.
            return packageUrl.resolve(packageSubpath);
        }
        // 11. Will Throw a Module Not Found error.
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
    private URI packageSelfResolve(String packageName, String packageSubpath ,URI parentURL, TruffleLanguage.Env env) {
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
            // 5.1. Return the result of PACKAGE_EXPORTS_RESOLVE(packageURL, packageSubpath, pjson.exports, defaultConditions).
            return packageExportsResolve(packageUrl, packageSubpath, pjson.getExportsProperty(), getConditions(), env);
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

        boolean hasTypeProperty() {
            if (hasNonNullProperty(jsonObj, TYPE)){
                Object nameValue = JSObject.get(jsonObj, TYPE);
                if (nameValue instanceof TruffleString nameStr) {
                    String type = nameStr.toString();
                    if(type.equals(TYPE_MODULE) || type.equals(TYPE_COMMONS_JS)){
                        return true;
                    }
                }
            }
            return false;
        }

        String getTypeProperty() {
            assert hasTypeProperty();
            Object nameValue = JSObject.get(jsonObj, TYPE);
            if (nameValue instanceof TruffleString nameStr) {
                return nameStr.toString();
            }
            return null;
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

        public Object getExportsProperty() {
            assert hasExportsProperty();
            return JSObject.get(jsonObj, EXPORTS_PROPERTY_NAME);
        }

        public boolean hasImportsProperty() {
            return hasNonNullProperty(jsonObj, IMPORTS_PROPERTY_NAME) && (JSObject.get(jsonObj, IMPORTS_PROPERTY_NAME) instanceof JSDynamicObject);
        }

        public JSDynamicObject getImportsProperty() {
            assert hasImportsProperty();
            return (JSDynamicObject) JSObject.get(jsonObj, IMPORTS_PROPERTY_NAME);
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
