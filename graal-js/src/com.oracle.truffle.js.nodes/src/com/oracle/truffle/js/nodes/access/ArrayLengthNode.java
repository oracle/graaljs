/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.ArrayLengthNodeFactory.ArrayLengthReadNodeGen;
import com.oracle.truffle.js.nodes.access.ArrayLengthNodeFactory.SetArrayLengthNodeGen;
import com.oracle.truffle.js.nodes.access.ArrayLengthNodeFactory.SetArrayLengthOrDeleteNodeGen;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.objects.JSObject;

@ImportStatic(ScriptArray.class)
public abstract class ArrayLengthNode extends JavaScriptBaseNode {

    protected static final int MAX_TYPE_COUNT = 3;

    protected ArrayLengthNode() {
    }

    protected static ScriptArray getArrayType(DynamicObject target, boolean condition) {
        return JSObject.getArray(target, condition);
    }

    protected abstract static class ArrayLengthReadNode extends ArrayLengthNode {

        public static ArrayLengthReadNode create() {
            return ArrayLengthReadNodeGen.create();
        }

        public abstract int executeInt(DynamicObject target, boolean condition) throws UnexpectedResultException;

        public abstract Object executeObject(DynamicObject target, boolean condition);

        public final double executeDouble(DynamicObject target, boolean condition) {
            Object result = executeObject(target, condition);
            if (result instanceof Integer) {
                return (int) result;
            }
            return (double) result;
        }

        @Specialization(guards = {"arrayType.isInstance(getArrayType(target, condition))", "arrayType.isStatelessType()", "isLengthAlwaysInt(arrayType)"}, limit = "MAX_TYPE_COUNT")
        protected static int doIntLength(DynamicObject target, boolean condition, //
                        @Cached("getArrayType(target, condition)") ScriptArray arrayType) {
            return arrayType.lengthInt(target, condition);
        }

        @Specialization(guards = {"arrayType.isInstance(getArrayType(target, condition))", "arrayType.isStatelessType()"}, replaces = "doIntLength", limit = "MAX_TYPE_COUNT")
        protected static double doLongLength(DynamicObject target, boolean condition, //
                        @Cached("getArrayType(target, condition)") ScriptArray arrayType) {
            return arrayType.length(target, condition);
        }

        @Specialization(replaces = "doLongLength")
        protected static double doGeneric(DynamicObject target, boolean condition) {
            return getArrayType(target, condition).length(target, condition);
        }

        protected static boolean isLengthAlwaysInt(ScriptArray arrayType) {
            return !(arrayType instanceof SparseArray);
        }
    }

    public abstract static class ArrayLengthWriteNode extends ArrayLengthNode {
        public static ArrayLengthWriteNode create(boolean strict) {
            return SetArrayLengthNodeGen.create(strict);
        }

        public static ArrayLengthWriteNode createSetOrDelete(boolean strict) {
            return SetArrayLengthOrDeleteNodeGen.create(strict);
        }

        public abstract void executeVoid(DynamicObject array, int length, boolean condition);
    }

    public abstract static class SetArrayLengthNode extends ArrayLengthWriteNode {
        private final boolean strict;

        protected SetArrayLengthNode(boolean strict) {
            this.strict = strict;
        }

        @Specialization(guards = {"arrayType.isInstance(getArrayType(arrayObj, condition))", "arrayType.isStatelessType()"}, limit = "MAX_TYPE_COUNT")
        protected void doCached(DynamicObject arrayObj, int length, boolean condition,
                        @Cached("getArrayType(arrayObj, condition)") ScriptArray arrayType,
                        @Cached("createSetLengthProfile()") ScriptArray.ProfileHolder setLengthProfile) {
            assert length >= 0;
            arraySetArrayType(arrayObj, arrayType.setLength(arrayObj, length, strict, condition, setLengthProfile));
        }

        @Specialization(replaces = "doCached")
        protected void doGeneric(DynamicObject arrayObj, int length, boolean condition,
                        @Cached("createSetLengthProfile()") ScriptArray.ProfileHolder setLengthProfile) {
            assert length >= 0;
            ScriptArray arrayType = getArrayType(arrayObj, condition);
            arraySetArrayType(arrayObj, arrayType.setLength(arrayObj, length, strict, condition, setLengthProfile));
        }
    }

    public abstract static class SetArrayLengthOrDeleteNode extends ArrayLengthWriteNode {
        private final boolean strict;

        protected SetArrayLengthOrDeleteNode(boolean strict) {
            this.strict = strict;
        }

        @Specialization(guards = {"arrayType.isInstance(getArrayType(arrayObj, condition))", "arrayType.isStatelessType()"}, limit = "MAX_TYPE_COUNT")
        protected void doCached(DynamicObject arrayObj, int length, boolean condition,
                        @Cached("getArrayType(arrayObj, condition)") ScriptArray arrayType,
                        @Cached("createSetLengthProfile()") ScriptArray.ProfileHolder setLengthProfile) {
            assert length >= 0;
            ScriptArray array = arrayType;
            if (arrayType.isLengthNotWritable()) {
                for (int i = array.lengthInt(arrayObj, condition) - 1; i >= length; i--) {
                    array = array.deleteElement(arrayObj, i, strict, condition);
                    arraySetArrayType(arrayObj, array);
                }
            }
            arraySetArrayType(arrayObj, array.setLength(arrayObj, length, strict, condition, setLengthProfile));
        }

        @Specialization(replaces = "doCached")
        protected void doGeneric(DynamicObject arrayObj, int length, boolean condition,
                        @Cached("createBinaryProfile()") ConditionProfile lengthNotWritableProfile,
                        @Cached("createSetLengthProfile()") ScriptArray.ProfileHolder setLengthProfile) {
            assert length >= 0;
            ScriptArray array = getArrayType(arrayObj, condition);
            if (lengthNotWritableProfile.profile(array.isLengthNotWritable())) {
                for (int i = array.lengthInt(arrayObj, condition) - 1; i >= length; i--) {
                    array = array.deleteElement(arrayObj, i, strict, condition);
                    arraySetArrayType(arrayObj, array);
                }
            }
            arraySetArrayType(arrayObj, array.setLength(arrayObj, length, strict, condition, setLengthProfile));
        }
    }
}
