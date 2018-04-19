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
            if (array.isLengthNotWritable() || array.isSealed()) {
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
            if (lengthNotWritableProfile.profile(array.isLengthNotWritable() || array.isSealed())) {
                for (int i = array.lengthInt(arrayObj, condition) - 1; i >= length; i--) {
                    array = array.deleteElement(arrayObj, i, strict, condition);
                    arraySetArrayType(arrayObj, array);
                }
            }
            arraySetArrayType(arrayObj, array.setLength(arrayObj, length, strict, condition, setLengthProfile));
        }
    }
}
