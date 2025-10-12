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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltins.JSArrayFromNode;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltins.JSArrayFunctionOperation;
import com.oracle.truffle.js.builtins.TypedArrayFunctionBuiltinsFactory.TypedArrayFromNodeGen;
import com.oracle.truffle.js.builtins.TypedArrayFunctionBuiltinsFactory.TypedArrayOfNodeGen;
import com.oracle.truffle.js.nodes.access.GetIteratorFromMethodNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Contains functions of the %TypedArray% constructor function object.
 */
public final class TypedArrayFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TypedArrayFunctionBuiltins.TypedArrayFunction> {

    public static final JSBuiltinsContainer BUILTINS = new TypedArrayFunctionBuiltins();

    protected TypedArrayFunctionBuiltins() {
        super(JSArrayBufferView.CLASS_NAME, TypedArrayFunction.class);
    }

    public enum TypedArrayFunction implements BuiltinEnum<TypedArrayFunction> {
        of(0),
        from(1);

        private final int length;

        TypedArrayFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TypedArrayFunction builtinEnum) {
        switch (builtinEnum) {
            case of:
                return TypedArrayOfNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case from:
                return TypedArrayFromNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class TypedArrayOfNode extends JSArrayFunctionOperation {
        public TypedArrayOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Specialization(guards = "isConstructor.executeBoolean(thisObj)")
        protected JSTypedArrayObject arrayOf(Object thisObj, Object[] args) {
            int len = args.length;
            JSTypedArrayObject newObj = getArraySpeciesConstructorNode().typedArrayCreateInWriteMode(thisObj, len);
            int k = 0;
            while (k < len) {
                Object kValue = args[k];
                write(newObj, k, kValue);
                k++;
            }
            return newObj;
        }

        @Fallback
        protected Object notConstructor(Object thisObj, @SuppressWarnings("unused") Object args) {
            throw Errors.createTypeErrorNotAConstructor(thisObj, getContext());
        }
    }

    public abstract static class TypedArrayFromNode extends JSArrayFromNode {

        public TypedArrayFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Override
        @Specialization(guards = "isConstructor.executeBoolean(thisObj)")
        protected Object arrayFrom(Object thisObj, Object source, Object mapFn, Object thisArg,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethod,
                        @Cached InlinedBranchProfile growBranch) {
            return arrayFromCommon(thisObj, source, mapFn, thisArg, false, getIteratorFromMethod, growBranch);
        }

        @Fallback
        @SuppressWarnings("unused")
        protected Object notConstructor(Object thisObj, Object source, Object mapFn, Object thisArg) {
            throw Errors.createTypeErrorNotAConstructor(thisObj, getContext());
        }

        @Override
        protected JSDynamicObject arrayFromIterable(Object thisObj, Object items, Object usingIterator, Object mapFn, Object thisArg, boolean mapping,
                        GetIteratorFromMethodNode getIteratorFromMethod, InlinedBranchProfile growProfile) {
            SimpleArrayList<Object> values = new SimpleArrayList<>();

            IteratorRecord iteratorRecord = getIteratorFromMethod.execute(this, items, usingIterator);
            while (true) {
                Object next = iteratorStep(iteratorRecord);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = getIteratorValue(next);
                values.add(nextValue, this, growProfile);
            }
            int len = values.size();
            JSTypedArrayObject obj = getArraySpeciesConstructorNode().typedArrayCreateInWriteMode(thisObj, len);
            for (int k = 0; k < len; k++) {
                Object mapped = values.get(k);
                if (mapping) {
                    mapped = callMapFn(thisArg, mapFn, mapped, k);
                }
                write(obj, k, mapped);
            }
            return obj;
        }
    }
}
