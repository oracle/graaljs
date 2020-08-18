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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSValueObject;

@ExportLibrary(InteropLibrary.class)
public final class JSNumberObject extends JSValueObject {
    public static final String CLASS_NAME = "Number";
    public static final String PROTOTYPE_NAME = "Number.prototype";

    private final Number number;

    protected JSNumberObject(Shape shape, Number number) {
        super(shape);
        this.number = number;
    }

    protected JSNumberObject(JSRealm realm, JSObjectFactory factory, Number number) {
        super(realm, factory);
        this.number = number;
    }

    public Number getNumber() {
        return number;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static DynamicObject create(Shape shape, Number value) {
        return new JSNumberObject(shape, value);
    }

    public static DynamicObject create(JSRealm realm, JSObjectFactory factory, Number value) {
        return new JSNumberObject(realm, factory, value);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isNumber() {
        return true;
    }

    @ExportMessage
    public boolean fitsInByte(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return numberLib.fitsInByte(JSNumber.valueOf(this));
    }

    @ExportMessage
    public boolean fitsInShort(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return numberLib.fitsInShort(JSNumber.valueOf(this));
    }

    @ExportMessage
    public boolean fitsInInt(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return numberLib.fitsInInt(JSNumber.valueOf(this));
    }

    @ExportMessage
    public boolean fitsInLong(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return numberLib.fitsInLong(JSNumber.valueOf(this));
    }

    @ExportMessage
    public boolean fitsInFloat(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return numberLib.fitsInFloat(JSNumber.valueOf(this));
    }

    @ExportMessage
    public boolean fitsInDouble(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return numberLib.fitsInDouble(JSNumber.valueOf(this));
    }

    @ExportMessage
    public byte asByte(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInByte(numberLib)) {
            return numberLib.asByte(JSNumber.valueOf(this));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public short asShort(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInShort(numberLib)) {
            return numberLib.asShort(JSNumber.valueOf(this));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public int asInt(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInInt(numberLib)) {
            return numberLib.asInt(JSNumber.valueOf(this));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public long asLong(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInLong(numberLib)) {
            return numberLib.asLong(JSNumber.valueOf(this));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public float asFloat(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInFloat(numberLib)) {
            return numberLib.asFloat(JSNumber.valueOf(this));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public double asDouble(
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInDouble(numberLib)) {
            return numberLib.asDouble(JSNumber.valueOf(this));
        } else {
            throw UnsupportedMessageException.create();
        }
    }
}
