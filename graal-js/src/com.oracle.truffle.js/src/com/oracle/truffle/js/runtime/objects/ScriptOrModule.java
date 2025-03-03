/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;

/**
 * Script or Module Record.
 */
public class ScriptOrModule {
    protected final JSContext context;
    protected final Source source;

    /**
     * Cache of imported module sources to keep alive sources referenced by this module in order to
     * prevent premature code cache GC of this module's dependencies.
     */
    private volatile Map<TruffleString, Source> importSourceCache;

    public ScriptOrModule(JSContext context, Source source) {
        this.context = context;
        this.source = source;
    }

    public final JSContext getContext() {
        return context;
    }

    public final Source getSource() {
        return source;
    }

    @TruffleBoundary
    public AbstractModuleRecord addLoadedModule(JSRealm realm, ModuleRequest moduleRequest, AbstractModuleRecord moduleRecord) {
        return realm.getModuleLoader().addLoadedModule(moduleRequest, moduleRecord);
    }

    /**
     * Keep a link from the referencing module or script to the imported module's {@link Source}, so
     * that the latter is kept alive for the lifetime of the former.
     */
    public void rememberImportedModuleSource(TruffleString moduleSpecifier, Source moduleSource) {
        // Note: the source might change, so we only remember the last source.
        getImportSourceCache().put(moduleSpecifier, moduleSource);
    }

    private Map<TruffleString, Source> getImportSourceCache() {
        Map<TruffleString, Source> cache = importSourceCache;
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            if (!IMPORT_SOURCE_CACHE_HANDLE.compareAndSet(this, (Map<TruffleString, Source>) null, cache)) {
                cache = importSourceCache;
            }
        }
        return cache;
    }

    private static final VarHandle IMPORT_SOURCE_CACHE_HANDLE;

    static {
        try {
            IMPORT_SOURCE_CACHE_HANDLE = MethodHandles.lookup().findVarHandle(ScriptOrModule.class, "importSourceCache", Map.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }
}
