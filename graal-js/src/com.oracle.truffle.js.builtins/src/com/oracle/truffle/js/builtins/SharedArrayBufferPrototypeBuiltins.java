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
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
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
