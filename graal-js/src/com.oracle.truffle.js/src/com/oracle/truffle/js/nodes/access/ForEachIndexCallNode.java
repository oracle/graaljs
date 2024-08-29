/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.array.JSArrayFirstElementIndexNode;
import com.oracle.truffle.js.nodes.array.JSArrayLastElementIndexNode;
import com.oracle.truffle.js.nodes.array.JSArrayNextElementIndexNode;
import com.oracle.truffle.js.nodes.array.JSArrayPreviousElementIndexNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

public abstract class ForEachIndexCallNode extends JavaScriptBaseNode {

    public abstract static class CallbackNode extends JavaScriptBaseNode {
        public abstract Object apply(long index, Object value, Object target, Object callback, Object callbackThisArg, Object currentResult);
    }

    @ValueType
    public static final class MaybeResult<T> {
        private final T result;
        private final boolean resultPresent;

        public MaybeResult(T result, boolean resultPresent) {
            this.result = result;
            this.resultPresent = resultPresent;
        }

        public static <T> MaybeResult<T> returnResult(T result) {
            return new MaybeResult<>(result, true);
        }

        public static <T> MaybeResult<T> continueResult(T result) {
            return new MaybeResult<>(result, false);
        }

        public boolean isPresent() {
            return resultPresent;
        }

        public T get() {
            return result;
        }
    }

    public abstract static class MaybeResultNode extends JavaScriptBaseNode {
        public abstract MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult);
    }

    @Child private IsArrayNode isArrayNode = IsArrayNode.createIsAnyArray();
    protected final JSClassProfile targetClassProfile = JSClassProfile.create();
    protected final LoopConditionProfile loopCond = LoopConditionProfile.create();
    protected final BranchProfile detachedBufferBranch = BranchProfile.create();
    @Child private CallbackNode callbackNode;
    @Child protected MaybeResultNode maybeResultNode;

    @Child private ReadElementNode.ReadElementArrayDispatchNode readElementNode;
    @Child private JSArrayFirstElementIndexNode firstElementIndexNode;
    @Child private JSArrayLastElementIndexNode lastElementIndexNode;
    @Child private JSHasPropertyNode hasPropertyNode;
    @Child private ImportValueNode toJSTypeNode;
    @Child private InteropLibrary interop;
    protected final JSContext context;
    protected final boolean checkHasProperty;

    protected ForEachIndexCallNode(JSContext context, CallbackNode callbackArgumentsNode, MaybeResultNode maybeResultNode, boolean checkHasProperty) {
        this.callbackNode = callbackArgumentsNode;
        this.maybeResultNode = maybeResultNode;
        this.context = context;
        this.checkHasProperty = checkHasProperty;
        this.readElementNode = ReadElementNode.ReadElementArrayDispatchNode.create();
    }

    public static ForEachIndexCallNode create(JSContext context, CallbackNode callbackArgumentsNode, MaybeResultNode maybeResultNode, boolean forward, boolean checkHasProperty) {
        if (forward) {
            return new ForwardForEachIndexCallNode(context, callbackArgumentsNode, maybeResultNode, checkHasProperty);
        } else {
            return new BackwardForEachIndexCallNode(context, callbackArgumentsNode, maybeResultNode, checkHasProperty);
        }
    }

    public final Object executeForEachIndex(Object target, Object callback, Object callbackThisArg, long fromIndex, long length, Object initialResult) {
        boolean isArray = isArrayNode.execute(target);
        if (isArray && context.getArrayPrototypeNoElementsAssumption().isValid()) {
            return executeForEachIndexFast((JSDynamicObject) target, callback, callbackThisArg, fromIndex, length, initialResult);
        } else {
            return executeForEachIndexSlow(target, callback, callbackThisArg, fromIndex, length, initialResult);
        }
    }

    protected abstract Object executeForEachIndexFast(JSDynamicObject target, Object callback, Object callbackThisArg, long fromIndex, long length, Object initialResult);

    protected abstract Object executeForEachIndexSlow(Object target, Object callback, Object callbackThisArg, long fromIndex, long length, Object initialResult);

    protected final long firstElementIndex(JSDynamicObject target, long length) {
        if (firstElementIndexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            firstElementIndexNode = insert(JSArrayFirstElementIndexNode.create(context));
        }
        return firstElementIndexNode.executeLong(target, length);
    }

    protected final long lastElementIndex(JSDynamicObject target, long length) {
        if (lastElementIndexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastElementIndexNode = insert(JSArrayLastElementIndexNode.create(context));
        }
        return lastElementIndexNode.executeLong(target, length);
    }

    protected final InteropLibrary getInterop() {
        if (interop == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            interop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
        }
        return interop;
    }

    protected Object foreignRead(Object target, long index, boolean isForeignArray) {
        if (toJSTypeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toJSTypeNode = insert(ImportValueNode.create());
        }
        if (isForeignArray) {
            return JSInteropUtil.readArrayElementOrDefault(target, index, Undefined.instance, getInterop(), toJSTypeNode, this);
        } else {
            return JSInteropUtil.readMemberOrDefault(target, Strings.fromLong(index), Undefined.instance, getInterop(), toJSTypeNode, this);
        }
    }

    protected Object getElement(Object target, long index, boolean isForeign, boolean isForeignArray) {
        if (!isForeign) {
            assert JSDynamicObject.isJSDynamicObject(target);
            return JSObject.get((JSDynamicObject) target, index, targetClassProfile);
        } else {
            return foreignRead(target, index, isForeignArray);
        }
    }

    protected final boolean hasDetachedBuffer(Object view) {
        return !context.getTypedArrayNotDetachedAssumption().isValid() && JSArrayBufferView.isJSArrayBufferView(view) && JSArrayBufferView.hasDetachedBuffer((JSDynamicObject) view);
    }

    protected final Object callback(long index, Object value, Object target, Object callback, Object callbackThisArg, Object currentResult) {
        if (callbackNode == null) {
            TruffleSafepoint.poll(this);
            return callbackThisArg;
        } else {
            // no safepoint polling necessary, call cares for that
            return callbackNode.apply(index, value, target, callback, callbackThisArg, currentResult);
        }
    }

    protected final Object readElementInBounds(JSDynamicObject target, long index) {
        return readElementNode.executeArrayGet(target, JSObject.getArray(target), index, target, Undefined.instance, context);
    }

    protected final boolean hasProperty(Object target, long index) {
        if (hasPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasPropertyNode = insert(JSHasPropertyNode.create());
        }
        return hasPropertyNode.executeBoolean(target, index);
    }

    protected static final class ForwardForEachIndexCallNode extends ForEachIndexCallNode {
        private final ConditionProfile fromIndexZero = ConditionProfile.create();

        @Child private JSArrayNextElementIndexNode nextElementIndexNode;

        public ForwardForEachIndexCallNode(JSContext context, CallbackNode callbackArgumentsNode, MaybeResultNode maybeResultNode, boolean checkHasProperty) {
            super(context, callbackArgumentsNode, maybeResultNode, checkHasProperty);
        }

        @Override
        protected Object executeForEachIndexFast(JSDynamicObject target, Object callback, Object callbackThisArg, long fromIndex, long length, Object initialResult) {
            long index = fromIndexZero.profile(fromIndex == 0) ? firstElementIndex(target, length) : nextElementIndex(target, fromIndex - 1, length);
            Object currentResult = initialResult;
            long count = 0;
            while (loopCond.profile(index < length && index <= lastElementIndex(target, length))) {
                if (checkHasProperty && hasDetachedBuffer(target)) {
                    detachedBufferBranch.enter();
                    break; // detached buffer does not have numeric properties
                }
                Object value = readElementInBounds(target, index);
                Object callbackResult = callback(index, value, target, callback, callbackThisArg, currentResult);
                MaybeResult<Object> maybeResult = maybeResultNode.apply(index, value, callbackResult, currentResult);
                currentResult = maybeResult.get();
                if (maybeResult.isPresent()) {
                    break;
                }
                count++;
                index = nextElementIndex(target, index, length);
            }
            reportLoopCount(this, count);
            return currentResult;
        }

        @Override
        protected Object executeForEachIndexSlow(Object target, Object callback, Object callbackThisArg, long fromIndex, long length, Object initialResult) {
            Object currentResult = initialResult;
            boolean isForeign = JSRuntime.isForeignObject(target);
            boolean isForeignArray = isForeign && getInterop().hasArrayElements(target);
            for (long index = fromIndex; index < length; index++) {
                if (!checkHasProperty || hasProperty(target, index)) {
                    Object value = getElement(target, index, isForeign, isForeignArray);
                    Object callbackResult = callback(index, value, target, callback, callbackThisArg, currentResult);
                    MaybeResult<Object> maybeResult = maybeResultNode.apply(index, value, callbackResult, currentResult);
                    currentResult = maybeResult.get();
                    if (maybeResult.isPresent()) {
                        break;
                    }
                }
            }
            reportLoopCount(this, length - fromIndex);
            return currentResult;
        }

        private long nextElementIndex(JSDynamicObject target, long currentIndex, long length) {
            if (nextElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nextElementIndexNode = insert(JSArrayNextElementIndexNode.create(context));
            }
            return nextElementIndexNode.executeLong(target, currentIndex, length);
        }
    }

    protected static final class BackwardForEachIndexCallNode extends ForEachIndexCallNode {
        @Child protected JSArrayPreviousElementIndexNode previousElementIndexNode;

        public BackwardForEachIndexCallNode(JSContext context, CallbackNode callbackArgumentsNode, MaybeResultNode maybeResultNode, boolean checkHasProperty) {
            super(context, callbackArgumentsNode, maybeResultNode, checkHasProperty);
        }

        @Override
        protected Object executeForEachIndexFast(JSDynamicObject target, Object callback, Object callbackThisArg, long fromIndex, long length, Object initialResult) {
            assert fromIndex < length;
            long index = previousElementIndex(target, fromIndex + 1);
            // NB: cannot rely on lastElementIndex here: can be > length (e.g. arguments object)
            Object currentResult = initialResult;
            long count = 0;
            while (loopCond.profile(index >= 0 && index >= firstElementIndex(target, length))) {
                if (checkHasProperty && hasDetachedBuffer(target)) {
                    detachedBufferBranch.enter();
                    break; // detached buffer does not have numeric properties
                }
                Object value = readElementInBounds(target, index);
                Object callbackResult = callback(index, value, target, callback, callbackThisArg, currentResult);
                MaybeResult<Object> maybeResult = maybeResultNode.apply(index, value, callbackResult, currentResult);
                currentResult = maybeResult.get();
                if (maybeResult.isPresent()) {
                    break;
                }
                count++;
                index = previousElementIndex(target, index);
            }
            reportLoopCount(this, count);
            return currentResult;
        }

        @Override
        protected Object executeForEachIndexSlow(Object target, Object callback, Object callbackThisArg, long fromIndex, long length, Object initialResult) {
            Object currentResult = initialResult;
            boolean isForeign = JSRuntime.isForeignObject(target);
            boolean isForeignArray = isForeign && getInterop().hasArrayElements(target);
            for (long index = fromIndex; index >= 0; index--) {
                if (!checkHasProperty || hasProperty(target, index)) {
                    Object value = getElement(target, index, isForeign, isForeignArray);
                    Object callbackResult = callback(index, value, target, callback, callbackThisArg, currentResult);
                    MaybeResult<Object> maybeResult = maybeResultNode.apply(index, value, callbackResult, currentResult);
                    currentResult = maybeResult.get();
                    if (maybeResult.isPresent()) {
                        break;
                    }
                }
            }
            reportLoopCount(this, fromIndex);
            return currentResult;
        }

        private long previousElementIndex(JSDynamicObject target, long currentIndex) {
            if (previousElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                previousElementIndexNode = insert(JSArrayPreviousElementIndexNode.create(context));
            }
            return previousElementIndexNode.executeLong(target, currentIndex);
        }
    }
}
