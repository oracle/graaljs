/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.DefaultEsModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public final class NpmCompatibleEsModuleLoader extends DefaultEsModuleLoader {

    public static NpmCompatibleEsModuleLoader create(JSRealm realm) {
        return new NpmCompatibleEsModuleLoader(realm);
    }

    private NpmCompatibleEsModuleLoader(JSRealm realm) {
        super(realm);
    }

    /**
     * Node.js-compatible implementation of ES modules loading.
     *
     * @link https://nodejs.org/api/esm.html#esm_import_specifiers
     * @link https://nodejs.org/api/esm.html#esm_resolver_algorithm
     *
     * @param referencingModule
     * @param specifier
     * @return
     */
    @TruffleBoundary
    @Override
    public JSModuleRecord resolveImportedModule(ScriptOrModule referencingModule, String specifier) {
        String resolvedUrl = resolveURL(referencingModule, specifier);
        try {
            TruffleFile file = getTruffleFile(resolvedUrl);
            return loadModuleFromUrl(specifier, file, resolvedUrl);
        } catch (IOException e) {
            throw Errors.createErrorFromException(e);
        }
    }

    private TruffleFile getTruffleFile(String resolvedUrl) throws IOException {
        assert resolvedUrl != null;
        return realm.getEnv().getPublicTruffleFile(resolvedUrl).getCanonicalFile();
    }

    private String resolveURL(ScriptOrModule referencingModule, String specifier) {
        // 1. Let resolvedURL be undefined.
        String resolvedUrl = null;

        // 2. If specifier is a valid URL, then
        try {
            new URL(specifier);
            // currently we don't support URLs
            throw new UnsupportedOperationException("TODO");
        } catch (MalformedURLException e) {
            // not an URL. Keep parsing.
        }
        // 3. Otherwise, if specifier starts with "/", then
        if (specifier.charAt(0) == '/') {
            // 3.1 Throw an Invalid Module Specifier error.
            throw fail(specifier);

        // 4. Otherwise, if specifier starts with "./" or "../", then
        } else if (isPathFileName(specifier)) {
            // 4.1 Set resolvedURL to the URL resolution of specifier relative to parentURL.
            String refPath = referencingModule == null ? null : referencingModule.getSource().getPath();
            if (refPath == null) {
                refPath = realm.getContext().getContextOptions().getRequireCwd();
            }
            resolvedUrl = refPath + "/" + specifier;

        // 5. Otherwise
        } else {
            // 5.1 Note: specifier is now a bare specifier.
            // 5.2 Set resolvedURL the result of PACKAGE_RESOLVE(specifier, parentURL).
            throw new UnsupportedOperationException("TODO");
        }

        assert resolvedUrl != null;
        // 6. If resolvedURL contains any percent encodings of "/" or "\" ("%2f" and "%5C" respectively), then
        if (resolvedUrl.contains("%2f") || resolvedUrl.contains("%5C")) {
            // 6.1 Throw an Invalid Module Specifier error.
            fail(specifier);
        }

        // 7. If resolvedURL does not end with a trailing "/" and the file at resolvedURL does not exist, then
        if (!resolvedUrl.endsWith("/") && !realm.getEnv().getPublicTruffleFile(resolvedUrl).exists()) {
            // 7.1 Throw an Invalid Module Specifier error.
            fail(specifier);
        }

        return resolvedUrl;
    }


    @Override
    public JSModuleRecord loadModule(Source moduleSource) {
        return null;
    }

    @TruffleBoundary
    private static JSException fail(String moduleIdentifier) {
        return JSException.create(JSErrorType.TypeError, "Cannot find module: '" + moduleIdentifier + "'");
    }

    private static boolean isPathFileName(String moduleIdentifier) {
        return moduleIdentifier.startsWith("/") || moduleIdentifier.startsWith("./") || moduleIdentifier.startsWith("../");
    }
}
