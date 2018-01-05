/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.util.ArrayList;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.ArrayBufferViewFunctionBuiltinsFactory.JSArrayBufferViewFromNodeGen;
import com.oracle.truffle.js.builtins.ArrayBufferViewFunctionBuiltinsFactory.JSArrayBufferViewOfNodeGen;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltins.JSArrayFromNode;
import com.oracle.truffle.js.builtins.ArrayFunctionBuiltins.JSArrayFunctionOperation;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

/**
 * Contains builtins for {@linkplain JSArrayBufferView} function (constructor).
 */
public final class ArrayBufferViewFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<ArrayBufferViewFunctionBuiltins.ArrayBufferViewFunction> {
    protected ArrayBufferViewFunctionBuiltins() {
        super(JSArrayBufferView.CLASS_NAME, ArrayBufferViewFunction.class);
    }

    public enum ArrayBufferViewFunction implements BuiltinEnum<ArrayBufferViewFunction> {
        of(0),
        from(1);

        private final int length;

        ArrayBufferViewFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ArrayBufferViewFunction builtinEnum) {
        switch (builtinEnum) {
            case of:
                return JSArrayBufferViewOfNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case from:
                return JSArrayBufferViewFromNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSArrayBufferViewOfNode extends JSArrayFunctionOperation {
        public JSArrayBufferViewOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Specialization
        protected DynamicObject arrayOf(Object thisObj, Object... args) {
            if (!isTypedArrayConstructor(thisObj)) {
                throw Errors.createTypeError("TypedArray expected");
            }
            int len = args.length;
            DynamicObject newObj = getArraySpeciesConstructorNode().typedArrayCreate((DynamicObject) thisObj, len);
            int k = 0;
            while (k < len) {
                Object kValue = args[k];
                write(newObj, k, kValue);
                k++;
            }
            return newObj;
        }
    }

    public abstract static class JSArrayBufferViewFromNode extends JSArrayFromNode {

        public JSArrayBufferViewFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, true);
        }

        @Override
        @Specialization
        protected DynamicObject arrayFrom(Object thisObj, Object[] args) {
            Object source = JSRuntime.getArgOrUndefined(args, 0);
            Object mapFn = JSRuntime.getArgOrUndefined(args, 1);
            Object thisArg = JSRuntime.getArgOrUndefined(args, 2);

            if (!JSFunction.isConstructor(thisObj)) {
                throw Errors.createTypeError("constructor expected");
            }
            return arrayFromIntl(thisObj, source, mapFn, thisArg, false);
        }

        @Override
        protected DynamicObject arrayFromIterable(Object thisObj, DynamicObject items, Object usingIterator, Object mapFn, Object thisArg, boolean mapping) {
            ArrayList<Object> values = new ArrayList<>();

            DynamicObject iterator = getIterator(items, usingIterator);
            while (true) {
                Object next = iteratorStep(iterator);
                if (next instanceof Boolean && ((Boolean) next) == Boolean.FALSE) {
                    break;
                }
                Object nextValue = getIteratorValue((DynamicObject) next);
                Boundaries.listAdd(values, nextValue);
            }
            int len = Boundaries.listSize(values);
            DynamicObject obj = getArraySpeciesConstructorNode().typedArrayCreate((DynamicObject) thisObj, len);
            for (int k = 0; k < len; k++) {
                Object mapped = Boundaries.listGet(values, k);
                if (mapping) {
                    mapped = callMapFn(thisArg, (DynamicObject) mapFn, new Object[]{mapped, k});
                }
                writeOwn(obj, k, mapped);
            }
            return obj;
        }
    }
}
