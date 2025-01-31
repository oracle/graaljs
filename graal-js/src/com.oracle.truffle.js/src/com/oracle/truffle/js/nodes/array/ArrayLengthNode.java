/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.array;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.array.ArrayLengthNodeFactory.ArrayLengthReadNodeGen;
import com.oracle.truffle.js.nodes.array.ArrayLengthNodeFactory.SetArrayLengthNodeGen;
import com.oracle.truffle.js.nodes.array.ArrayLengthNodeFactory.SetArrayLengthOrDeleteNodeGen;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBase;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

@ImportStatic(ScriptArray.class)
public abstract class ArrayLengthNode extends JavaScriptBaseNode {

    protected static final int MAX_TYPE_COUNT = 4;

    protected ArrayLengthNode() {
    }

    @NeverDefault
    protected static ScriptArray getArrayType(JSDynamicObject target) {
        return JSObject.getArray(target);
    }

    public abstract static class ArrayLengthReadNode extends ArrayLengthNode {

        @NeverDefault
        public static ArrayLengthReadNode create() {
            return ArrayLengthReadNodeGen.create();
        }

        public abstract int executeInt(JSDynamicObject target) throws UnexpectedResultException;

        public abstract Object executeObject(JSDynamicObject target);

        public final double executeDouble(JSDynamicObject target) {
            Object result = executeObject(target);
            if (result instanceof Integer) {
                return (int) result;
            }
            return (double) result;
        }

        @Specialization(guards = {"!target.hasAutoLength()"})
        protected static int doTypedArrayFixedLength(JSTypedArrayObject target) {
            return target.getLengthFixed();
        }

        @Specialization(guards = {"target.hasAutoLength()"})
        protected final int doTypedArrayAutoLength(JSTypedArrayObject target,
                        @Cached TypedArrayLengthNode typedArrayLengthNode) {
            return typedArrayLengthNode.execute(this, target, getJSContext());
        }

        @Specialization(guards = {"arrayType.isInstance(target.getArrayType())", "isLengthAlwaysInt(arrayType)"}, limit = "1")
        protected static int doIntLength(JSArrayBase target,
                        @Cached("getArrayType(target)") ScriptArray arrayType) {
            return arrayType.lengthInt(target);
        }

        @Specialization(replaces = {"doIntLength"}, rewriteOn = UnexpectedResultException.class)
        protected static int doUncachedIntLength(JSArrayBase target) throws UnexpectedResultException {
            long uint32Len = JSAbstractArray.arrayGetLength(target);
            assert uint32Len == getArrayType(target).length(target);
            if (JSRuntime.longIsRepresentableAsInt(uint32Len)) {
                return (int) uint32Len;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnexpectedResultException((double) uint32Len);
            }
        }

        @Specialization(replaces = {"doUncachedIntLength"})
        protected static double doUncachedLongLength(JSArrayBase target) {
            long uint32Len = JSAbstractArray.arrayGetLength(target);
            assert uint32Len == getArrayType(target).length(target);
            return uint32Len;
        }

        @Idempotent
        protected static boolean isLengthAlwaysInt(ScriptArray arrayType) {
            return !(arrayType instanceof SparseArray);
        }
    }

    public abstract static class ArrayLengthWriteNode extends ArrayLengthNode {
        @NeverDefault
        public static ArrayLengthWriteNode create(boolean strict) {
            return SetArrayLengthNodeGen.create(strict);
        }

        @NeverDefault
        public static ArrayLengthWriteNode createSetOrDelete(boolean strict) {
            return SetArrayLengthOrDeleteNodeGen.create(strict);
        }

        public abstract void executeVoid(JSArrayObject array, int length);
    }

    public abstract static class SetArrayLengthNode extends ArrayLengthWriteNode {
        private final boolean strict;

        protected SetArrayLengthNode(boolean strict) {
            this.strict = strict;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = {"arrayType.isInstance(getArrayType(arrayObj))"}, limit = "MAX_TYPE_COUNT")
        protected void doCached(JSArrayObject arrayObj, int length,
                        @Bind Node node,
                        @Cached("getArrayType(arrayObj)") ScriptArray arrayType,
                        @Cached @Shared ScriptArray.SetLengthProfileAccess setLengthProfile) {
            assert length >= 0;
            if (arrayType.isSealed()) {
                setLengthSealed(arrayObj, length, arrayType, node, setLengthProfile);
                return;
            }
            arraySetArrayType(arrayObj, arrayType.setLength(arrayObj, length, strict, node, setLengthProfile));
        }

        @Specialization(replaces = "doCached")
        protected void doGeneric(JSArrayObject arrayObj, int length,
                        @Cached InlinedConditionProfile sealedProfile,
                        @Cached @Shared ScriptArray.SetLengthProfileAccess setLengthProfile) {
            assert length >= 0;
            ScriptArray arrayType = getArrayType(arrayObj);
            if (sealedProfile.profile(this, arrayType.isSealed())) {
                setLengthSealed(arrayObj, length, arrayType, this, setLengthProfile);
                return;
            }
            arraySetArrayType(arrayObj, arrayType.setLength(arrayObj, length, strict, this, setLengthProfile));
        }

        private void setLengthSealed(JSArrayObject arrayObj, int length, ScriptArray arrayType, Node node, ScriptArray.SetLengthProfileAccess setLengthProfile) {
            long minLength = arrayType.lastElementIndex(arrayObj) + 1;
            if (length < minLength) {
                ScriptArray array = arrayType.setLength(arrayObj, minLength, strict, node, setLengthProfile);
                arraySetArrayType(arrayObj, array);
                array.canDeleteElement(arrayObj, minLength - 1, strict);
                return;
            }
            arraySetArrayType(arrayObj, arrayType.setLength(arrayObj, length, strict, node, setLengthProfile));
        }
    }

    public abstract static class SetArrayLengthOrDeleteNode extends ArrayLengthWriteNode {
        private final boolean strict;

        protected SetArrayLengthOrDeleteNode(boolean strict) {
            this.strict = strict;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = {"arrayType.isInstance(getArrayType(arrayObj))"}, limit = "MAX_TYPE_COUNT")
        protected void doCached(JSArrayObject arrayObj, int length,
                        @Bind Node node,
                        @Cached("getArrayType(arrayObj)") ScriptArray arrayType,
                        @Cached @Shared ScriptArray.SetLengthProfileAccess setLengthProfile) {
            assert length >= 0;
            if (arrayType.isLengthNotWritable() || arrayType.isSealed()) {
                deleteAndSetLength(arrayObj, length, arrayType, node, setLengthProfile);
                return;
            }
            arraySetArrayType(arrayObj, arrayType.setLength(arrayObj, length, strict, node, setLengthProfile));
        }

        @Specialization(replaces = "doCached")
        protected void doGeneric(JSArrayObject arrayObj, int length,
                        @Cached InlinedConditionProfile mustDeleteProfile,
                        @Cached @Shared ScriptArray.SetLengthProfileAccess setLengthProfile) {
            assert length >= 0;
            ScriptArray arrayType = getArrayType(arrayObj);
            if (mustDeleteProfile.profile(this, arrayType.isLengthNotWritable() || arrayType.isSealed())) {
                deleteAndSetLength(arrayObj, length, arrayType, this, setLengthProfile);
                return;
            }
            arraySetArrayType(arrayObj, arrayType.setLength(arrayObj, length, strict, this, setLengthProfile));
        }

        private void deleteAndSetLength(JSArrayObject arrayObj, int length, ScriptArray arrayType, Node node, ScriptArray.SetLengthProfileAccess setLengthProfile) {
            ScriptArray array = arrayType;
            for (long i = array.length(arrayObj) - 1; i >= length; i--) {
                if (array.canDeleteElement(arrayObj, i, strict)) {
                    array = array.deleteElement(arrayObj, i, strict);
                    arraySetArrayType(arrayObj, array);
                }
            }
            arraySetArrayType(arrayObj, array.setLength(arrayObj, length, strict, node, setLengthProfile));
        }
    }
}
