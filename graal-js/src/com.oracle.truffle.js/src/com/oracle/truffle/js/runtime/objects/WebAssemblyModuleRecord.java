/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * WebAssembly Module Record.
 */
public class WebAssemblyModuleRecord extends AbstractModuleRecord {

    private final JSWebAssemblyModuleObject webAssemblyModule;

    public WebAssemblyModuleRecord(JSContext context, Source source, JSWebAssemblyModuleObject webAssemblyModule) {
        super(context, source, null);
        this.webAssemblyModule = webAssemblyModule;
    }

    @Override
    public Object getModuleSource() {
        return webAssemblyModule;
    }

    @Override
    public JSPromiseObject loadRequestedModules(JSRealm realm, Object hostDefined) {
        throw Errors.createSyntaxError("Unsupported");
    }

    @Override
    public void link(JSRealm realm) {
        throw Errors.createSyntaxError("Unsupported");
    }

    @Override
    public Object evaluate(JSRealm realm) {
        throw Errors.createSyntaxError("Unsupported");
    }

    @Override
    public Collection<TruffleString> getExportedNames(Set<JSModuleRecord> exportStarSet) {
        throw Errors.createSyntaxError("Unsupported");
    }

    @Override
    public ExportResolution resolveExport(TruffleString exportName, Set<Pair<? extends AbstractModuleRecord, TruffleString>> resolveSet) {
        throw Errors.createSyntaxError("Unsupported");
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "WebAssembly.Module" + "@" + Integer.toHexString(System.identityHashCode(this)) + "[source=" + getSource() + "]";
    }
}
