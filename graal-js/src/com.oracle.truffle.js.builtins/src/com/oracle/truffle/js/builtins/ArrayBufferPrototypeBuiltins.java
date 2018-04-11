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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.ArrayBufferPrototypeBuiltinsFactory.JSArrayBufferSliceNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerSpecialNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSArrayBuffer}.prototype.
 */
public final class ArrayBufferPrototypeBuiltins extends JSBuiltinsContainer.Lambda {
    public ArrayBufferPrototypeBuiltins() {
        super(JSArrayBuffer.PROTOTYPE_NAME);
        defineFunction("slice", 2, (context, builtin) -> JSArrayBufferSliceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context)));
    }

    public abstract static class JSArrayBufferOperation extends JSBuiltinNode {

        public JSArrayBufferOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToIntegerSpecialNode toIntegerNode;

        protected long toInteger(Object thisObject) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(JSToIntegerSpecialNode.create());
            }
            return toIntegerNode.executeLong(thisObject);
        }
    }

    public abstract static class JSArrayBufferAbstractSliceNode extends JSArrayBufferOperation {

        @Child private ArraySpeciesConstructorNode arraySpeciesCreateNode;

        public JSArrayBufferAbstractSliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected int getStart(Object start, int len) {
            long relativeStart = toInteger(start);
            if (relativeStart < 0) {
                return (int) Math.max((len + relativeStart), 0);
            } else {
                return (int) Math.min(relativeStart, len);
            }
        }

        protected int getEnd(Object end, int len) {
            long relativeEnd = end == Undefined.instance ? len : toInteger(end);
            if (relativeEnd < 0) {
                return (int) Math.max((len + relativeEnd), 0);
            } else {
                return (int) Math.min(relativeEnd, len);
            }
        }

        /**
         * Clamp index to range [lowerBound,upperBound]. A negative index refers from upperBound.
         */
        protected static int clampIndex(int index, int lowerBound, int upperBound) {
            return clamp(index >= 0 ? index : index + upperBound, lowerBound, upperBound);
        }

        /**
         * Clamp index to range [lowerBound,upperBound].
         */
        private static int clamp(int index, int lowerBound, int upperBound) {
            return Math.max(Math.min(index, upperBound), lowerBound);
        }

        public ArraySpeciesConstructorNode getArraySpeciesConstructorNode() {
            if (arraySpeciesCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arraySpeciesCreateNode = insert(ArraySpeciesConstructorNode.create(getContext(), true));
            }
            return arraySpeciesCreateNode;
        }

        @TruffleBoundary
        protected static void sliceDirectIntl(ByteBuffer byteBuffer, int clampedBegin, int clampedEnd, ByteBuffer resBuffer) {
            ByteBuffer slice = ((ByteBuffer) byteBuffer.duplicate().position(clampedBegin).limit(clampedEnd));
            assert resBuffer.position() == 0;
            resBuffer.duplicate().put(slice);
        }

    }

    public abstract static class JSArrayBufferSliceNode extends JSArrayBufferAbstractSliceNode {

        private final BranchProfile errorBranch = BranchProfile.create();

        public JSArrayBufferSliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        /**
         * ArrayBuffer slice(long begin, optional long end).
         *
         * Returns a new ArrayBuffer whose contents are a copy of this ArrayBuffer's bytes from
         * begin, inclusive, up to end, exclusive. If either begin or end is negative, it refers to
         * an index from the end of the array, as opposed to from the beginning.
         *
         * If end is unspecified, the new ArrayBuffer contains all bytes from begin to the end of
         * this ArrayBuffer.
         *
         * The range specified by the begin and end values is clamped to the valid index range for
         * the current array. If the computed length of the new ArrayBuffer would be negative, it is
         * clamped to zero.
         *
         * @param thisObj ArrayBuffer
         * @param begin begin index
         * @param end end index
         * @return sliced ArrayBuffer
         */
        @Specialization(guards = "isJSHeapArrayBuffer(thisObj)")
        protected DynamicObject slice(DynamicObject thisObj, int begin, int end) {
            byte[] byteArray = JSArrayBuffer.getByteArray(thisObj);
            int clampedBegin = clampIndex(begin, 0, byteArray.length);
            int clampedEnd = clampIndex(end, clampedBegin, byteArray.length);
            int newLen = Math.max(clampedEnd - clampedBegin, 0);

            DynamicObject resObj = constructNewArrayBuffer(thisObj, newLen);
            checkErrors(resObj, thisObj, newLen, false);

            byte[] newByteArray = JSArrayBuffer.getByteArray(resObj);
            System.arraycopy(byteArray, clampedBegin, newByteArray, 0, newLen);
            return resObj;
        }

        private DynamicObject constructNewArrayBuffer(DynamicObject thisObj, int newLen) {
            DynamicObject defaultConstructor = getContext().getRealm().getArrayBufferConstructor().getFunctionObject();
            DynamicObject constr = getArraySpeciesConstructorNode().speciesConstructor(thisObj, defaultConstructor);
            return (DynamicObject) getArraySpeciesConstructorNode().construct(constr, newLen);
        }

        private void checkErrors(DynamicObject resObj, DynamicObject thisObj, int newLen, boolean direct) {
            if ((direct && !JSArrayBuffer.isJSDirectArrayBuffer(resObj)) || (!direct && !JSArrayBuffer.isJSHeapArrayBuffer(resObj))) {
                errorBranch.enter();
                throw Errors.createTypeErrorArrayBufferExpected();
            }
            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(resObj)) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
            if (resObj == thisObj) {
                errorBranch.enter();
                throw Errors.createTypeError("SameValue(new, O) is forbidden");
            }
            if ((direct && JSArrayBuffer.getDirectByteLength(resObj) < newLen) || (!direct && JSArrayBuffer.getByteLength(resObj) < newLen)) {
                errorBranch.enter();
                throw Errors.createTypeError("insufficient length constructed");
            }
            // NOTE: Side-effects of the above steps may have detached O.
            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(thisObj)) {
                // yes, check again! see clause 22 of ES 6 24.1.4.3.
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }

        @Specialization(guards = "isJSHeapArrayBuffer(thisObj)")
        protected DynamicObject slice(DynamicObject thisObj, Object begin0, Object end0) {
            int len = JSArrayBuffer.getByteArray(thisObj).length;
            int begin = getStart(begin0, len);
            int finalEnd = getEnd(end0, len);
            return slice(thisObj, begin, finalEnd);
        }

        @Specialization(guards = "isJSDirectArrayBuffer(thisObj)")
        protected DynamicObject sliceDirect(DynamicObject thisObj, int begin, int end) {
            ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(thisObj);
            int byteLength = JSArrayBuffer.getDirectByteLength(thisObj);
            int clampedBegin = clampIndex(begin, 0, byteLength);
            int clampedEnd = clampIndex(end, clampedBegin, byteLength);
            int newLen = clampedEnd - clampedBegin;

            DynamicObject resObj = constructNewArrayBuffer(thisObj, newLen);
            checkErrors(resObj, thisObj, newLen, true);

            ByteBuffer resBuffer = JSArrayBuffer.getDirectByteBuffer(resObj);
            sliceDirectIntl(byteBuffer, clampedBegin, clampedEnd, resBuffer);
            return resObj;
        }

        @Specialization(guards = "isJSDirectArrayBuffer(thisObj)")
        protected DynamicObject sliceDirect(DynamicObject thisObj, Object begin0, Object end0) {
            int len = JSArrayBuffer.getDirectByteLength(thisObj);
            int begin = getStart(begin0, len);
            int end = getEnd(end0, len);
            return sliceDirect(thisObj, begin, end);
        }

        @Specialization(guards = {"!isJSHeapArrayBuffer(thisObj)", "!isJSDirectArrayBuffer(thisObj)"})
        protected static DynamicObject error(Object thisObj, @SuppressWarnings("unused") Object begin0, @SuppressWarnings("unused") Object end0) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }
    }
}
