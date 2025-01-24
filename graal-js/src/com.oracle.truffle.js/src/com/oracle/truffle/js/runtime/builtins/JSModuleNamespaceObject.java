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
package com.oracle.truffle.js.runtime.builtins;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.objects.AbstractModuleRecord;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.objects.Null;

public final class JSModuleNamespaceObject extends JSNonProxyObject {
    /**
     * [[Module]]. Module Record.
     *
     * The Module Record whose exports this namespace exposes.
     */
    private final AbstractModuleRecord module;

    /**
     * [[Exports]]. List of String.
     *
     * A List containing the String values of the exported names exposed as own properties of this
     * object. The list is ordered as if an Array of those String values had been sorted using
     * Array.prototype.sort using SortCompare as comparefn.
     */
    private final UnmodifiableEconomicMap<TruffleString, ExportResolution> exports;

    protected JSModuleNamespaceObject(Shape shape, AbstractModuleRecord module, UnmodifiableEconomicMap<TruffleString, ExportResolution> exports) {
        super(shape, Null.instance);
        this.module = module;
        this.exports = exports;
    }

    public AbstractModuleRecord getModule() {
        return module;
    }

    public UnmodifiableEconomicMap<TruffleString, ExportResolution> getExports() {
        return exports;
    }

    public static JSModuleNamespaceObject create(JSRealm realm, JSObjectFactory factory, AbstractModuleRecord module, UnmodifiableEconomicMap<TruffleString, ExportResolution> exports) {
        return factory.initProto(new JSModuleNamespaceObject(factory.getShape(realm), module, exports), realm);
    }

    @Override
    public TruffleString getClassName() {
        return JSModuleNamespace.CLASS_NAME;
    }

    @Override
    public boolean setPrototypeOf(JSDynamicObject newPrototype) {
        return newPrototype == Null.instance;
    }

    @TruffleBoundary
    @Override
    public boolean testIntegrityLevel(boolean frozen) {
        return testIntegrityLevel(frozen, false);
    }

    @Override
    @TruffleBoundary
    public boolean setIntegrityLevel(boolean freeze, boolean doThrow) {
        return testIntegrityLevel(freeze, true);
    }

    private boolean testIntegrityLevel(boolean frozen, boolean doThrow) {
        for (ExportResolution binding : getExports().getValues()) {
            // Check for uninitialized binding (throws ReferenceError)
            JSModuleNamespace.getBindingValue(binding);
            if (frozen) {
                if (doThrow) {
                    // ReferenceError if the first binding is uninitialized, TypeError otherwise
                    throw Errors.createTypeError("not allowed to freeze a namespace object");
                }
                return false;
            }
        }
        return true;
    }

    @Override
    @TruffleBoundary
    public TruffleString toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        return Strings.addBrackets(JSModuleNamespace.CLASS_NAME);
    }
}
