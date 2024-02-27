/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;

/**
 * Basic non-proxy JS object.
 *
 * @see com.oracle.truffle.js.runtime.builtins.JSNonProxy
 */
@ExportLibrary(InteropLibrary.class)
public abstract class JSNonProxyObject extends JSClassObject {

    protected JSNonProxyObject(Shape shape, JSDynamicObject proto) {
        super(shape, proto);
    }

    @ExportMessage
    public final boolean hasMetaObject() {
        return getMetaObjectImpl() != null;
    }

    @ExportMessage
    public final Object getMetaObject() throws UnsupportedMessageException {
        Object metaObject = getMetaObjectImpl();
        if (metaObject != null) {
            return metaObject;
        }
        throw UnsupportedMessageException.create();
    }

    @TruffleBoundary
    public final Object getMetaObjectImpl() {
        assert !JSGuards.isJSProxy(this);
        Object metaObject = JSRuntime.getDataProperty(this, JSObject.CONSTRUCTOR);
        if (metaObject != null && metaObject instanceof JSFunctionObject && ((JSFunctionObject) metaObject).isMetaInstance(this)) {
            return metaObject;
        }
        return null;
    }

    @Override
    public final boolean isExtensible() {
        return JSNonProxy.ordinaryIsExtensible(this);
    }

    @Override
    public boolean preventExtensions(boolean doThrow) {
        return JSNonProxy.ordinaryPreventExtensions(this, 0);
    }

    @Override
    public boolean testIntegrityLevel(boolean frozen) {
        if (JSShape.usesOrdinaryGetOwnProperty(getShape())) {
            return JSNonProxy.testIntegrityLevelFast(this, frozen);
        }
        return super.testIntegrityLevel(frozen);
    }

    @Override
    public boolean setIntegrityLevel(boolean freeze, boolean doThrow) {
        if (JSShape.usesOrdinaryGetOwnProperty(getShape())) {
            return JSNonProxy.setIntegrityLevelFast(this, freeze);
        }
        return super.setIntegrityLevel(freeze, doThrow);
    }

    @Override
    public TruffleString getClassName() {
        return Strings.UC_OBJECT;
    }

    @TruffleBoundary
    @Override
    public TruffleString toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return defaultToString();
        } else {
            return JSRuntime.objectToDisplayString(this, allowSideEffects, format, depth, getClassName());
        }
    }
}
