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
package com.oracle.truffle.js.nodes.cast;

import java.util.Set;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Converts an arbitrary array-like object to an Object[], with an optional array length limit.
 * <p>
 * By {@link #create() default}, throws {@code TypeError} for {@code null} and {@code undefined}
 * (can be changed by using {@link #create(boolean)}). Throws a RangeError if the array length
 * exceeds the optional array length limit or the maximum Java array size.
 *
 * @see #nullOrUndefinedAsEmptyArray
 */
@ImportStatic({JSConfig.class})
public abstract class JSToObjectArrayNode extends JavaScriptBaseNode {

    protected final boolean nullOrUndefinedAsEmptyArray;

    protected JSToObjectArrayNode(boolean nullOrUndefinedAsEmptyArray) {
        this.nullOrUndefinedAsEmptyArray = nullOrUndefinedAsEmptyArray;
    }

    public final Object[] executeObjectArray(Object value) {
        return executeObjectArray(value, JSConfig.SOFT_MAX_ARRAY_LENGTH);
    }

    public abstract Object[] executeObjectArray(Object value, int arrayLengthLimit);

    @NeverDefault
    public static JSToObjectArrayNode create() {
        return create(false);
    }

    @NeverDefault
    public static JSToObjectArrayNode create(boolean nullOrUndefinedAsEmptyArray) {
        return JSToObjectArrayNodeGen.create(nullOrUndefinedAsEmptyArray);
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode operand) {
        class Unary extends JSUnaryNode {
            @Child private JSToObjectArrayNode toObjectArray = JSToObjectArrayNode.create();

            Unary(JavaScriptNode operandNode) {
                super(operandNode);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return toObjectArray.executeObjectArray(operandNode.execute(frame), context.getLanguageOptions().maxApplyArgumentLength());
            }

            @Override
            protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
                return new Unary(cloneUninitialized(getOperand(), materializedTags));
            }
        }
        return new Unary(operand);
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization
    protected Object[] toArray(JSObject obj, int arrayLengthLimit,
                    @Bind Node node,
                    @Cached("create(getJSContext())") JSGetLengthNode getLengthNode,
                    @Cached("create(getJSContext())") ReadElementNode readNode,
                    @Cached @Shared InlinedBranchProfile errorBranch) {
        long len = getLengthNode.executeLong(obj);
        if (len > arrayLengthLimit) {
            errorBranch.enter(node);
            throw Errors.createRangeErrorTooManyArguments();
        }
        int iLen = (int) len;
        assert JSRuntime.longIsRepresentableAsInt(len);

        Object[] arr = new Object[iLen];
        for (int index = 0; index < iLen; index++) {
            Object value = readNode.executeWithTargetAndIndex(obj, index);
            arr[index] = value;
        }
        return arr;
    }

    @Specialization(guards = "isUndefined(value)")
    protected Object[] doUndefined(Object value, @SuppressWarnings("unused") int arrayLengthLimit) {
        return emptyArrayOrObjectError(value);
    }

    @Specialization(guards = "isJSNull(value)")
    protected Object[] doNull(Object value, @SuppressWarnings("unused") int arrayLengthLimit) {
        return emptyArrayOrObjectError(value);
    }

    private Object[] emptyArrayOrObjectError(Object value) {
        if (nullOrUndefinedAsEmptyArray) {
            return ScriptArray.EMPTY_OBJECT_ARRAY;
        }
        throw Errors.createTypeErrorNotAnObject(value);
    }

    @Specialization
    protected Object[] passArray(Object[] array, int arrayLengthLimit,
                    @Cached @Shared InlinedBranchProfile errorBranch) {
        if (array.length > arrayLengthLimit) {
            errorBranch.enter(this);
            throw Errors.createRangeErrorTooManyArguments();
        }
        return array;
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"isForeignObject(obj)"}, limit = "InteropLibraryLimit")
    protected Object[] doForeignObject(Object obj, int arrayLengthLimit,
                    @Bind Node node,
                    @CachedLibrary("obj") InteropLibrary interop,
                    @Cached @Shared InlinedBranchProfile errorBranch,
                    @Cached ImportValueNode foreignConvertNode) {
        try {
            if (!interop.hasArrayElements(obj)) {
                errorBranch.enter(node);
                throw Errors.createTypeError("foreign object must be an array", this);
            }

            long len = interop.getArraySize(obj);
            if (len > arrayLengthLimit) {
                errorBranch.enter(node);
                throw Errors.createRangeErrorTooManyArguments();
            }
            int iLen = (int) len;
            Object[] arr = new Object[iLen];
            for (int i = 0; i < iLen; i++) {
                arr[i] = foreignConvertNode.executeWithTarget(interop.readArrayElement(obj, i));
            }
            return arr;
        } catch (InvalidArrayIndexException | UnsupportedMessageException e) {
            errorBranch.enter(node);
            throw Errors.createTypeErrorNotAnObject(obj);
        }
    }

    @Fallback
    protected Object[] doFallback(Object value, @SuppressWarnings("unused") int arrayLengthLimit) {
        assert !JSRuntime.isObject(value) && !JSRuntime.isNullOrUndefined(value);
        if (nullOrUndefinedAsEmptyArray && getJSContext().isOptionNashornCompatibilityMode()) {
            throw Errors.createTypeError("Function.prototype.apply expects an Array for second argument");
        } else {
            throw Errors.createTypeErrorNotAnObject(value);
        }
    }
}
