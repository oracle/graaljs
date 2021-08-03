/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.FileSystemException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.UserScriptException;

public class DefaultESModuleLoader implements JSModuleLoader {

    protected final JSRealm realm;
    protected final Map<String, JSModuleRecord> moduleMap = new HashMap<>();

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
            return uri.getScheme() != null ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public JSModuleRecord resolveImportedModule(ScriptOrModule referrer, String specifier) {
        String refPath = referrer == null ? null : referrer.getSource().getPath();
        try {
            TruffleFile moduleFile;
            URI maybeUri = asURI(specifier);
            if (refPath == null) {
                if (maybeUri != null) {
                    moduleFile = realm.getEnv().getPublicTruffleFile(maybeUri).getCanonicalFile();
                } else {
                    moduleFile = realm.getEnv().getPublicTruffleFile(specifier).getCanonicalFile();
                }
            } else {
                TruffleFile refFile = realm.getEnv().getPublicTruffleFile(refPath);
                if (maybeUri != null) {
                    String uriFile = realm.getEnv().getPublicTruffleFile(maybeUri).getCanonicalFile().getPath();
                    moduleFile = refFile.resolveSibling(uriFile).getCanonicalFile();
                } else {
                    moduleFile = refFile.resolveSibling(specifier).getCanonicalFile();
                }
            }
            String canonicalPath = moduleFile.getPath();
            return loadModuleFromUrl(referrer, specifier, moduleFile, canonicalPath);
        } catch (FileSystemException fsex) {
            String fileName = fsex.getFile();
            if (Objects.equals(fsex.getMessage(), fileName)) {
                String message = "Error reading: " + fileName;
                if (realm.getContext().isOptionV8CompatibilityMode()) {
                    // d8 throws string. We don't want to follow this bad practice outside V8
                    // compatibility mode.
                    throw UserScriptException.create(message);
                } else {
                    throw Errors.createError(message);
                }
            } else {
                // Use the original message when it doesn't seem useless
                throw Errors.createErrorFromException(fsex);
            }
        } catch (IOException | SecurityException e) {
            throw Errors.createErrorFromException(e);
        }
    }

    protected JSModuleRecord loadModuleFromUrl(ScriptOrModule referrer, String specifier, TruffleFile moduleFile, String canonicalPath) throws IOException {
        JSModuleRecord existingModule = moduleMap.get(canonicalPath);
        if (existingModule != null) {
            return existingModule;
        }
        Source source = Source.newBuilder(JavaScriptLanguage.ID, moduleFile).name(specifier).mimeType(JavaScriptLanguage.MODULE_MIME_TYPE).build();
        JSModuleData parsedModule = realm.getContext().getEvaluator().envParseModule(realm, source);
        JSModuleRecord newModule = new JSModuleRecord(parsedModule, this);
        moduleMap.put(canonicalPath, newModule);

        if (referrer instanceof JSModuleRecord) {
            ((JSModuleRecord) referrer).getModuleData().rememberImportedModuleSource(specifier, source);
        }
        return newModule;
    }

    @Override
    public JSModuleRecord loadModule(Source source, JSModuleData moduleData) {
        String canonicalPath = getCanonicalPath(source);
        return moduleMap.computeIfAbsent(canonicalPath, (key) -> new JSModuleRecord(moduleData, this));
    }

    private String getCanonicalPath(Source source) {
        String path = source.getPath();
        String canonicalPath;
        if (path == null) {
            // Source does not originate from a file.
            canonicalPath = source.getName();
        } else {
            try {
                TruffleFile moduleFile = realm.getEnv().getPublicTruffleFile(path);
                if (moduleFile.exists()) {
                    canonicalPath = moduleFile.getCanonicalFile().getPath();
                } else {
                    // Source with a non-existing path but with a content.
                    canonicalPath = path;
                }
            } catch (IOException | SecurityException e) {
                throw Errors.createErrorFromException(e);
            }
        }
        return canonicalPath;
    }
}
