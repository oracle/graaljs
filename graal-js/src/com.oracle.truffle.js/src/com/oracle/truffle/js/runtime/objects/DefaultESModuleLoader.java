/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.objects;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;

import com.oracle.js.parser.ir.Module.ImportPhase;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;

public class DefaultESModuleLoader implements JSModuleLoader {

    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String DOT_SLASH = "./";
    public static final String DOT_DOT_SLASH = "../";

    protected final JSRealm realm;
    protected final Map<String, AbstractModuleRecord> moduleMap = new HashMap<>();

    public static DefaultESModuleLoader create(JSRealm realm) {
        return new DefaultESModuleLoader(realm);
    }

    protected DefaultESModuleLoader(JSRealm realm) {
        this.realm = realm;
    }

    protected URI asURI(String specifier) {
        assert specifier != null;
        if (specifier.indexOf(':') == -1) {
            return null;
        }
        try {
            URI uri = new URI(specifier);
            return uri.isAbsolute() ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public AbstractModuleRecord resolveImportedModule(ScriptOrModule referrer, ModuleRequest moduleRequest) {
        String refPath = null;
        String refPathOrName = null;
        if (referrer != null) {
            Source referrerSource = referrer.getSource();
            refPath = referrerSource.getPath();
            refPathOrName = refPath != null ? refPath : referrerSource.getName();
        }
        try {
            TruffleString specifierTS = moduleRequest.specifier();
            String specifier = Strings.toJavaString(specifierTS);
            TruffleFile moduleFile;
            String canonicalPath;
            URI maybeUri = asURI(specifier);

            TruffleString maybeCustomPath = realm.getCustomEsmPathMapping(refPath == null ? null : Strings.fromJavaString(refPath), specifierTS);
            TruffleLanguage.Env env = realm.getEnv();
            if (maybeCustomPath != null) {
                canonicalPath = maybeCustomPath.toJavaStringUncached();
                moduleFile = getCanonicalFileIfExists(env.getPublicTruffleFile(canonicalPath), env);
            } else {
                if (refPath == null) {
                    if (maybeUri != null) {
                        moduleFile = env.getPublicTruffleFile(maybeUri);
                    } else {
                        moduleFile = env.getPublicTruffleFile(specifier);
                    }
                } else {
                    TruffleFile refFile = env.getPublicTruffleFile(refPath);
                    if (maybeUri != null) {
                        String uriFile = env.getPublicTruffleFile(maybeUri).getCanonicalFile().getPath();
                        moduleFile = refFile.resolveSibling(uriFile);
                    } else {
                        if (!env.isFileIOAllowed() || bareSpecifierDirectLookup(specifier)) {
                            moduleFile = env.getPublicTruffleFile(specifier);
                        } else {
                            moduleFile = refFile.resolveSibling(specifier);
                        }
                    }
                }
                canonicalPath = null;
            }
            return loadModuleFromUrl(referrer, moduleRequest, moduleFile, canonicalPath);
        } catch (FileSystemException fsex) {
            throw createErrorFromFileSystemException(fsex, refPathOrName);
        } catch (IOException | SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
            throw Errors.createErrorFromException(e);
        }
    }

    private JSException createErrorFromFileSystemException(FileSystemException fsex, String refPath) {
        String fileName = fsex.getFile();
        if (realm.getContext().getLanguageOptions().testV8Mode()) {
            String message = "d8: Error reading module from " + fileName;
            if (refPath != null) {
                message += " imported by " + refPath;
            }
            return Errors.createError(message, fsex);
        }
        String reason = fsex.getReason();
        String message = null;
        if (reason == null) {
            // Provide a more useful error message
            if (fsex instanceof NoSuchFileException) {
                message = "Cannot find module";
            } else if (fsex instanceof AccessDeniedException) {
                message = "Cannot access module";
            }
        }
        message = buildErrorMessage(message, fileName, refPath, reason);
        return Errors.createError(message, fsex);
    }

    private static String buildErrorMessage(String causeMessage, String fileName, String refPath, String reason) {
        String message = causeMessage;
        if (message == null) {
            message = "Error reading module";
        }
        message += " '" + fileName + "'";
        if (refPath != null) {
            message += " imported from " + refPath;
        }
        if (reason != null) {
            message += ": " + reason;
        }
        return message;
    }

    private static JSException createErrorUnsupportedPhase(ScriptOrModule referrer, ModuleRequest moduleRequest) {
        String refPath = referrer != null ? referrer.getSource().getName() : null;
        return Errors.createError(buildErrorMessage(null, moduleRequest.specifier().toJavaStringUncached(), refPath,
                        moduleRequest.phase() + " phase imports not supported for this type of module"));
    }

    private boolean bareSpecifierDirectLookup(String specifier) {
        var options = realm.getContext().getLanguageOptions();
        if (options.esmBareSpecifierRelativeLookup()) {
            return false;
        }
        return !(specifier.startsWith(SLASH) || specifier.startsWith(DOT_SLASH) || specifier.startsWith(DOT_DOT_SLASH));
    }

    protected AbstractModuleRecord loadModuleFromUrl(ScriptOrModule referrer, ModuleRequest moduleRequest, TruffleFile moduleFile, String maybeCanonicalPath) throws IOException {
        TruffleFile canonicalFile;
        String canonicalPath;
        TruffleLanguage.Env env = realm.getEnv();
        if (maybeCanonicalPath == null) {
            /*
             * We can only canonicalize the path if I/O is allowed and the file exists; otherwise,
             * the lookup may still succeed if the module was loaded already (as literal source).
             */
            canonicalFile = getCanonicalFileIfExists(moduleFile, env);
            canonicalPath = canonicalFile.getPath();
        } else {
            canonicalFile = moduleFile;
            canonicalPath = maybeCanonicalPath;
        }

        AbstractModuleRecord existingModule = moduleMap.get(canonicalPath);
        if (existingModule != null) {
            return existingModule;
        }

        String mimeType = findMimeType(canonicalFile);
        String language = findLanguage(mimeType);

        Source source = Source.newBuilder(language, canonicalFile).name(Strings.toJavaString(moduleRequest.specifier())).mimeType(mimeType).build();
        Map<TruffleString, TruffleString> attributes = moduleRequest.attributes();
        TruffleString assertedType = attributes.get(JSContext.getTypeImportAttribute());
        if (!doesModuleTypeMatchAssertionType(assertedType, mimeType)) {
            throw Errors.createTypeError("Invalid module type was asserted");
        }
        AbstractModuleRecord newModule = switch (mimeType) {
            case JavaScriptLanguage.JSON_MIME_TYPE -> realm.getContext().getEvaluator().parseJSONModule(realm, source);
            case JavaScriptLanguage.WASM_MIME_TYPE -> {
                if (moduleRequest.phase() == ImportPhase.Source && realm.getContextOptions().isWebAssembly()) {
                    yield realm.getContext().getEvaluator().parseWasmModuleSource(realm, source);
                } else {
                    throw createErrorUnsupportedPhase(referrer, moduleRequest);
                }
            }
            default -> {
                JSModuleData parsedModule = realm.getContext().getEvaluator().envParseModule(realm, source);
                yield new JSModuleRecord(parsedModule, this);
            }
        };

        moduleMap.put(canonicalPath, newModule);

        if (referrer != null) {
            referrer.rememberImportedModuleSource(moduleRequest.specifier(), source);
        }
        return newModule;
    }

    /**
     * Try to detect the mime type of the imported module, considering only supported mime types,
     * and assuming ES module by default.
     */
    private String findMimeType(TruffleFile moduleFile) {
        final String defaultMimeType = JavaScriptLanguage.MODULE_MIME_TYPE;
        if (moduleFile == null) {
            return defaultMimeType;
        }
        String foundMimeType;
        try {
            // Source.findMimeType may return null.
            foundMimeType = Source.findMimeType(moduleFile);
        } catch (IOException | SecurityException e) {
            foundMimeType = null;
        }
        if (foundMimeType == null) {
            foundMimeType = findMimeTypeFromExtension(moduleFile.getName());
        }
        return filterSupportedMimeType(foundMimeType, defaultMimeType);
    }

    private String filterSupportedMimeType(String foundMimeType, String defaultMimeType) {
        String mimeType = defaultMimeType;
        if (JavaScriptLanguage.JSON_MIME_TYPE.equals(foundMimeType)) {
            if (realm.getContextOptions().isJsonModules()) {
                mimeType = JavaScriptLanguage.JSON_MIME_TYPE;
            }
        } else if (JavaScriptLanguage.WASM_MIME_TYPE.equals(foundMimeType)) {
            mimeType = JavaScriptLanguage.WASM_MIME_TYPE;
        }
        return mimeType;
    }

    private static String findMimeTypeFromExtension(String moduleName) {
        if (moduleName.endsWith(JavaScriptLanguage.JSON_SOURCE_NAME_SUFFIX)) {
            return JavaScriptLanguage.JSON_MIME_TYPE;
        }
        if (moduleName.endsWith(JavaScriptLanguage.WASM_SOURCE_NAME_SUFFIX)) {
            return JavaScriptLanguage.WASM_MIME_TYPE;
        }
        return null;
    }

    /**
     * Like {@link Source#findLanguage(String)}, but considering only supported mime types.
     */
    private static String findLanguage(String mimeType) {
        String language = JavaScriptLanguage.ID;
        if (JavaScriptLanguage.WASM_MIME_TYPE.equals(mimeType)) {
            language = JavaScriptLanguage.WASM_LANGUAGE_ID;
        }
        return language;
    }

    private static boolean doesModuleTypeMatchAssertionType(TruffleString assertedType, String mimeType) {
        if (assertedType == null) {
            return true;
        }
        if (Strings.equals(Strings.JSON, assertedType)) {
            return mimeType.equals(JavaScriptLanguage.JSON_MIME_TYPE);
        }
        return false;
    }

    @Override
    public AbstractModuleRecord addLoadedModule(ModuleRequest moduleRequest, AbstractModuleRecord moduleRecord) {
        String canonicalPath = getCanonicalPath(moduleRecord.getSource());
        return moduleMap.putIfAbsent(canonicalPath, moduleRecord);
    }

    private String getCanonicalPath(Source source) {
        String path = source.getPath();
        String canonicalPath;
        if (path == null) {
            // Source does not originate from a file.
            canonicalPath = source.getName();
        } else {
            try {
                TruffleLanguage.Env env = realm.getEnv();
                if (env.getFileNameSeparator().equals("\\") && path.startsWith("/")) {
                    // on Windows, remove first "/" from /c:/test/dir/ style paths
                    path = path.substring(1);
                }
                TruffleFile moduleFile = env.getPublicTruffleFile(path);
                if (env.isFileIOAllowed() && moduleFile.exists()) {
                    try {
                        canonicalPath = moduleFile.getCanonicalFile().getPath();
                    } catch (NoSuchFileException ex) {
                        // The file may have been deleted between exists() and getCanonicalFile().
                        // We handle this race condition as if the file did not exist.
                        canonicalPath = path;
                    }
                } else {
                    // Source with a non-existing path but with a content.
                    canonicalPath = path;
                }
            } catch (IOException | SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
                throw Errors.createErrorFromException(e);
            }
        }
        return canonicalPath;
    }

    private static TruffleFile getCanonicalFileIfExists(TruffleFile file, TruffleLanguage.Env env) throws IOException {
        if (env.isFileIOAllowed() && file.exists()) {
            try {
                return file.getCanonicalFile();
            } catch (NoSuchFileException ex) {
                // The file may have been deleted between exists() and getCanonicalFile().
                // We handle this race condition as if the file did not exist.
            }
        }
        return file;
    }
}
