/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

@ExportLibrary(value = InteropLibrary.class, receiverType = DynamicObject.class)
public final class ForeignBoxedObject extends ObjectType {
    private static final Layout LAYOUT = Layout.createLayout();
    private static final ForeignBoxedObject SINGLETON = new ForeignBoxedObject();
    private static final DynamicObjectFactory FACTORY;
    private static final Property VALUE_PROPERTY;

    static {
        Shape.Allocator allocator = LAYOUT.createAllocator();
        Shape shape = LAYOUT.createShape(SINGLETON);
        VALUE_PROPERTY = Property.create("value", allocator.locationForType(Object.class), 0);
        FACTORY = shape.addProperty(VALUE_PROPERTY).createFactory();
    }

    private ForeignBoxedObject() {
    }

    public static DynamicObject createNew(Object value) {
        return FACTORY.newInstance(value);
    }

    private static Object getValue(DynamicObject object) {
        return VALUE_PROPERTY.getLocation().get(object);
    }

    @Override
    public Class<?> dispatch() {
        return ForeignBoxedObject.class;
    }

    @ExportMessage
    static boolean isBoolean(DynamicObject object) {
        return getValue(object) instanceof Boolean;
    }

    @ExportMessage
    static boolean asBoolean(DynamicObject object) throws UnsupportedMessageException {
        if (!isBoolean(object)) {
            throw UnsupportedMessageException.create();
        }
        return (boolean) getValue(object);
    }

    @ExportMessage
    static boolean isString(DynamicObject object) {
        return getValue(object) instanceof String;
    }

    @ExportMessage
    static String asString(DynamicObject object) throws UnsupportedMessageException {
        if (!isString(object)) {
            throw UnsupportedMessageException.create();
        }
        return (String) getValue(object);
    }

    @ExportMessage
    static boolean isNumber(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) {
        return interop.isNumber(getValue(receiver));
    }

    @ExportMessage
    static boolean fitsInByte(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) {
        return interop.fitsInByte(getValue(receiver));
    }

    @ExportMessage
    static boolean fitsInShort(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) {
        return interop.fitsInShort(getValue(receiver));
    }

    @ExportMessage
    static boolean fitsInInt(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) {
        return interop.fitsInInt(getValue(receiver));
    }

    @ExportMessage
    static boolean fitsInLong(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) {
        return interop.fitsInLong(getValue(receiver));
    }

    @ExportMessage
    static boolean fitsInFloat(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) {
        return interop.fitsInFloat(getValue(receiver));
    }

    @ExportMessage
    static boolean fitsInDouble(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) {
        return interop.fitsInDouble(getValue(receiver));
    }

    @ExportMessage
    static byte asByte(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asByte(getValue(receiver));
    }

    @ExportMessage
    static short asShort(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asShort(getValue(receiver));
    }

    @ExportMessage
    static int asInt(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asInt(getValue(receiver));
    }

    @ExportMessage
    static long asLong(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asLong(getValue(receiver));
    }

    @ExportMessage
    static float asFloat(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asFloat(getValue(receiver));
    }

    @ExportMessage
    static double asDouble(DynamicObject receiver,
                    @CachedLibrary(limit = "1") InteropLibrary interop) throws UnsupportedMessageException {
        return interop.asDouble(getValue(receiver));
    }
}
