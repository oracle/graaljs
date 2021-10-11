/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode;

import java.util.HashMap;
import java.util.Map;

import com.oracle.js.parser.ir.Module;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleLoader;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;

class ESModuleLoader implements JSModuleLoader {
    private final Map<ScriptOrModule, Map<String, JSModuleRecord>> cache = new HashMap<>();
    private long resolver;

    void setResolver(long resolver) {
        this.resolver = resolver;
    }

    @Override
    public JSModuleRecord resolveImportedModule(ScriptOrModule referrer, Module.ModuleRequest moduleRequest) {
        Map<String, JSModuleRecord> referrerCache = cache.get(referrer);
        String specifier = moduleRequest.getSpecifier();
        if (referrerCache == null) {
            referrerCache = new HashMap<>();
            cache.put(referrer, referrerCache);
        } else {
            JSModuleRecord cached = referrerCache.get(specifier);
            if (cached != null) {
                return cached;
            }
        }
        if (resolver == 0) {
            System.err.println("Cannot resolve module outside module instantiation!");
            System.exit(1);
        }
        JSRealm realm = JSRealm.get(null);
        JSModuleRecord result = (JSModuleRecord) NativeAccess.executeResolveCallback(resolver, realm, specifier, referrer);
        referrerCache.put(specifier, result);
        return result;
    }

    @Override
    public JSModuleRecord loadModule(Source moduleSource, JSModuleData moduleData) {
        throw new UnsupportedOperationException();
    }
}
