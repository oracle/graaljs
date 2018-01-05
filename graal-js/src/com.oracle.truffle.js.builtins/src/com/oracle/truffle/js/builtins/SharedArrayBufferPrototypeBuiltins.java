/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltins.JSArrayBufferAbstractSliceNode;
import com.oracle.truffle.js.builtins.SharedArrayBufferPrototypeBuiltinsFactory.JSSharedArrayBufferSliceNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;

/**
 * Contains builtins for {@linkplain JSSharedArrayBuffer}.prototype.
 */
public final class SharedArrayBufferPrototypeBuiltins extends JSBuiltinsContainer.Lambda {
    public SharedArrayBufferPrototypeBuiltins() {
        super(JSSharedArrayBuffer.PROTOTYPE_NAME);
        defineFunction("slice", 2, (context, builtin) -> JSSharedArrayBufferSliceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context)));
    }

    public abstract static class JSSharedArrayBufferSliceNode extends JSArrayBufferAbstractSliceNode {

        private final BranchProfile errorBranch = BranchProfile.create();

        public JSSharedArrayBufferSliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private DynamicObject constructNewSharedArrayBuffer(DynamicObject thisObj, int newLen) {
            DynamicObject defaultConstructor = getContext().getRealm().getSharedArrayBufferConstructor().getFunctionObject();
            DynamicObject constr = getArraySpeciesConstructorNode().speciesConstructor(thisObj, defaultConstructor);
            return (DynamicObject) getArraySpeciesConstructorNode().construct(constr, newLen);
        }

        private void checkErrors(DynamicObject resObj, DynamicObject thisObj, int newLen) {
            if (!JSSharedArrayBuffer.isJSSharedArrayBuffer(resObj)) {
                errorBranch.enter();
                throw Errors.createTypeError("SharedArrayBuffer expected");
            }
            if (!JSSharedArrayBuffer.hasArrayBufferData(thisObj)) {
                errorBranch.enter();
                throw Errors.createTypeError("cannot slice a null buffer");
            }
            if (resObj == thisObj) {
                errorBranch.enter();
                throw Errors.createTypeError("SameValue(new, O) is forbidden");
            }
            if (JSSharedArrayBuffer.getDirectByteBuffer(resObj).capacity() < newLen) {
                errorBranch.enter();
                throw Errors.createTypeError("insufficient length constructed");
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSharedArrayBuffer(thisObj)")
        protected static DynamicObject error(Object thisObj, Object begin0, Object end0) {
            throw Errors.createTypeError("Cannot execute on non-shared array", thisObj);
        }

        @Specialization(guards = "isJSSharedArrayBuffer(thisObj)")
        protected DynamicObject sliceSharedDirect(DynamicObject thisObj, int begin, int end) {
            ByteBuffer byteBuffer = JSSharedArrayBuffer.getDirectByteBuffer(thisObj);
            int byteLength = JSArrayBuffer.getDirectByteLength(thisObj);
            int clampedBegin = clampIndex(begin, 0, byteLength);
            int clampedEnd = clampIndex(end, clampedBegin, byteLength);
            int newLen = clampedEnd - clampedBegin;

            DynamicObject resObj = constructNewSharedArrayBuffer(thisObj, newLen);
            checkErrors(resObj, thisObj, newLen);

            ByteBuffer resBuffer = JSArrayBuffer.getDirectByteBuffer(resObj);
            sliceDirectIntl(byteBuffer, clampedBegin, clampedEnd, resBuffer);
            return resObj;
        }

        @Specialization(guards = "isJSSharedArrayBuffer(thisObj)")
        protected DynamicObject sliceSharedDirect(DynamicObject thisObj, Object begin0, Object end0) {
            int len = JSSharedArrayBuffer.getDirectByteBuffer(thisObj).capacity();
            int begin = getStart(begin0, len);
            int end = getEnd(end0, len);
            return sliceSharedDirect(thisObj, begin, end);
        }

    }
}
