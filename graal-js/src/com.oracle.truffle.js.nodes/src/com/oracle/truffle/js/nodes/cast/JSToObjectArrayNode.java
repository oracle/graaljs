/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import java.util.List;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSGetLengthNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

/**
 * Converts an arbitrary value to an Object[].
 * <p>
 * By {@link #create(JSContext) default}, throws {@code TypeError} for {@code null} or
 * {@code undefined} (can be changed by using {@link #create(JSContext, boolean)}).
 * </p>
 *
 * @see #nullOrUndefinedAsEmptyArray
 */
@ImportStatic(value = JSInteropUtil.class)
public abstract class JSToObjectArrayNode extends JavaScriptBaseNode {

    protected final JSContext context;
    protected final boolean nullOrUndefinedAsEmptyArray;

    protected JSToObjectArrayNode(JSContext context, boolean nullOrUndefinedAsEmptyArray) {
        this.context = Objects.requireNonNull(context);
        this.nullOrUndefinedAsEmptyArray = nullOrUndefinedAsEmptyArray;
    }

    public abstract Object[] executeObjectArray(Object value);

    public static JSToObjectArrayNode create(JSContext context) {
        return create(context, false);
    }

    public static JSToObjectArrayNode create(JSContext context, boolean nullOrUndefinedAsEmptyArray) {
        return JSToObjectArrayNodeGen.create(context, nullOrUndefinedAsEmptyArray);
    }

    @Specialization(guards = {"isJSObject(obj)"})
    protected static Object[] toArray(DynamicObject obj,
                    @Cached("create(context)") JSGetLengthNode getLengthNode,
                    @Cached("create(context)") ReadElementNode readNode) {
        long len = getLengthNode.executeLong(obj);
        if (len > JSTruffleOptions.MaxApplyArgumentLength) {
            CompilerDirectives.transferToInterpreter();
            throw Errors.createCallStackSizeExceededError();
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
    protected Object[] doUndefined(Object value) {
        return emptyArrayOrObjectError(value);
    }

    @Specialization(guards = "isJSNull(value)")
    protected Object[] doNull(Object value) {
        return emptyArrayOrObjectError(value);
    }

    @Specialization
    protected static Object[] toArrayString(CharSequence value) {
        return notAnObjectError(value);
    }

    @Specialization
    protected static Object[] toArrayInt(int value) {
        return notAnObjectError(value);
    }

    @Specialization
    protected static Object[] toArrayDouble(double value) {
        return notAnObjectError(value);
    }

    @Specialization
    protected static Object[] toArrayBoolean(boolean value) {
        return notAnObjectError(value);
    }

    private Object[] emptyArrayOrObjectError(Object value) {
        if (nullOrUndefinedAsEmptyArray) {
            return ScriptArray.EMPTY_OBJECT_ARRAY;
        }
        return notAnObjectError(value);
    }

    private static Object[] notAnObjectError(Object value) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            throw Errors.createTypeError("Function.prototype.apply expects an Array for second argument");
        } else {
            throw Errors.createTypeErrorNotAnObject(value);
        }
    }

    @Specialization
    protected static Object[] passArray(Object[] array) {
        if (array.length > JSTruffleOptions.MaxApplyArgumentLength) {
            CompilerDirectives.transferToInterpreter();
            throw Errors.createCallStackSizeExceededError();
        }
        return array;
    }

    @TruffleBoundary
    @Specialization(guards = "isList(value)")
    protected static Object[] doList(Object value) {
        List<?> list = ((List<?>) value);
        if (list.size() > JSTruffleOptions.MaxApplyArgumentLength) {
            CompilerDirectives.transferToInterpreter();
            throw Errors.createCallStackSizeExceededError();
        }
        return list.toArray();
    }

    @Specialization(guards = {"!isDynamicObject(obj)"})
    protected static Object[] doForeignObject(TruffleObject obj,
                    @Cached("create()") JSToNumberNode toNumberNode,
                    @Cached("create()") BranchProfile hasPropertiesBranch,
                    @Cached("createHasSize()") Node hasSizeNode,
                    @Cached("createGetSize()") Node getSizeNode,
                    @Cached("createRead()") Node readNode,
                    @Cached("create()") JSForeignToJSTypeNode foreignConvertNode) {
        try {
            if (!ForeignAccess.sendHasSize(hasSizeNode, obj)) {
                throw Errors.createTypeError("foreign Object reports not to have a SIZE");
            }

            long len = JSRuntime.toUInt32(toNumberNode.executeNumber(ForeignAccess.sendGetSize(getSizeNode, obj)));
            if (len > JSTruffleOptions.MaxApplyArgumentLength) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createCallStackSizeExceededError();
            }
            int iLen = (int) len;
            Object[] arr = new Object[iLen];
            if (len > 0) {
                hasPropertiesBranch.enter();
                for (int i = 0; i < iLen; i++) {
                    arr[i] = foreignConvertNode.executeWithTarget(ForeignAccess.sendRead(readNode, obj, i));
                }
            }
            return arr;
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            throw Errors.createTypeErrorNotAnObject(obj);
        }
    }

    @Fallback
    protected static Object[] doFallback(Object value) {
        assert !JSRuntime.isObject(value);
        return notAnObjectError(value);
    }
}
