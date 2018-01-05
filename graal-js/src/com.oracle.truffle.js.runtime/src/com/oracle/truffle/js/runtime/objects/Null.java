/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
