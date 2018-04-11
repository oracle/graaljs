/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.AbstractJSClass;
import com.oracle.truffle.js.runtime.builtins.JSClass;

public final class Null {

    public static final String TYPE_NAME = "object";
    public static final String NAME = "null";
    public static final JSClass NULL_CLASS = NullClass.INSTANCE;
    private static final Shape SHAPE = JSShape.makeStaticRoot(JSObject.LAYOUT, NULL_CLASS, 0);
    public static final String CLASS_NAME = "null|undefined";
    public static final DynamicObject instance = JSObject.create(SHAPE);

    private Null() {
    }

    static final class NullClass extends AbstractJSClass {
        static final NullClass INSTANCE = new NullClass();

        private NullClass() {
        }

        @Override
        public String getClassName(DynamicObject object) {
            return object == Undefined.instance ? Undefined.NAME : Null.NAME;
        }

        @Override
        public String toString() {
            return CLASS_NAME;
        }

        @Override
        public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
            return delete(thisObj, Boundaries.stringValueOf(index), isStrict);
        }

        @Override
        public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
            throw Errors.createTypeErrorCannotDeletePropertyOf(key, thisObj);
        }

        @Override
        public String safeToString(DynamicObject object) {
            return object == Undefined.instance ? "[object Undefined]" : "[object Null]";
        }

        @Override
        public String defaultToString(DynamicObject thisObj) {
            return thisObj == Undefined.instance ? Undefined.NAME : Null.NAME;
        }

        @Override
        @TruffleBoundary
        public String toString(DynamicObject object) {
            return "DynamicObject<" + defaultToString(object) + ">@" + Integer.toHexString(hashCode(object));
        }

        @Override
        public ForeignAccess getForeignAccessFactory(DynamicObject object) {
            return NullOrUndefinedForeignAccessFactory.getForeignAccess();
        }
    }
}
