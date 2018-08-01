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
package com.oracle.truffle.js.runtime.truffleinterop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Utility class for interop operations.
 *
 */
public final class JSInteropNodeUtil {
    private JSInteropNodeUtil() {
        // this class should not be instantiated
    }

    static Node getWriteNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createWrite();
    }

    static Node getReadNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createRead();
    }

    static Node getKeysNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createKeys();
    }

    static Node getGetSizeNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createGetSize();
    }

    static Node getIsNullNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createIsNull();
    }

    static Node getIsBoxedNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createIsBoxed();
    }

    static Node getUnboxNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createUnbox();
    }

    static Node getHasSizeNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createHasSize();
    }

    static Node getRemoveNode() {
        CompilerAsserts.neverPartOfCompilation();
        return JSInteropUtil.createRemove();
    }

    @TruffleBoundary
    public static Object getSize(TruffleObject obj) {
        return getSize(obj, getGetSizeNode());
    }

    public static Object getSize(TruffleObject obj, Node getSizeNode) {
        try {
            return ForeignAccess.sendGetSize(getSizeNode, obj);
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(obj, e, Message.GET_SIZE, null);
        }
    }

    @TruffleBoundary
    public static boolean hasSize(TruffleObject obj) {
        return hasSize(obj, getHasSizeNode());
    }

    public static boolean hasSize(TruffleObject obj, Node hasSizeNode) {
        return ForeignAccess.sendHasSize(hasSizeNode, obj);
    }

    @TruffleBoundary
    public static Object read(TruffleObject obj, Object key) {
        return JSRuntime.importValue(readRaw(obj, key));
    }

    @TruffleBoundary
    public static Object readRaw(TruffleObject obj, Object key) {
        return read(obj, key, getReadNode());
    }

    /**
     * Sends the READ message to obj and returns the result. Note that you might have to convert the
     * result to a JS type, {@link JSRuntime#importValue(Object)}.
     */
    public static Object read(TruffleObject obj, Object key, Node readNode) {
        try {
            return ForeignAccess.sendRead(readNode, obj, key);
        } catch (UnknownIdentifierException e) {
            return Undefined.instance;
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(obj, e, Message.READ, null);
        }
    }

    @TruffleBoundary
    public static Object write(TruffleObject obj, Object key, Object value) {
        return write(obj, key, value, getWriteNode());
    }

    public static Object write(TruffleObject obj, Object key, Object value, Node writeNode) {
        try {
            return ForeignAccess.sendWrite(writeNode, obj, key, value);
        } catch (UnknownIdentifierException | UnsupportedTypeException | UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(obj, e, Message.WRITE, null);
        }
    }

    @TruffleBoundary
    public static List<Object> keys(TruffleObject obj) {
        return keys(obj, getKeysNode(), getReadNode(), getGetSizeNode());
    }

    public static List<Object> keys(TruffleObject obj, Node keysNode, Node readNode, Node getSizeNode) {
        return keys(obj, keysNode, readNode, getSizeNode, false);
    }

    public static List<Object> keys(TruffleObject obj, Node keysNode, Node readNode, Node getSizeNode, boolean lenient) {
        try {
            return keysThrowsUME(obj, keysNode, readNode, getSizeNode);
        } catch (UnsupportedMessageException e) {
            if (lenient) {
                return Collections.emptyList();
            }
            throw Errors.createTypeErrorInteropException(obj, e, Message.KEYS, null);
        }
    }

    public static List<Object> keysThrowsUME(TruffleObject obj, Node keysNode, Node readNode, Node getSizeNode) throws UnsupportedMessageException {
        TruffleObject keysObj = ForeignAccess.sendKeys(keysNode, obj);
        int size = (int) JSRuntime.toLength(getSize(keysObj, getSizeNode));
        List<Object> keys = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Object key = read(keysObj, i, readNode); // will only read Strings
            keys.add(key);
        }
        return keys;
    }

    public static boolean hasProperty(TruffleObject obj, Object key, Node keyInfoNode) {
        return KeyInfo.isExisting(ForeignAccess.sendKeyInfo(keyInfoNode, obj, key));
    }

    @TruffleBoundary
    public static boolean hasProperty(TruffleObject obj, Object key) {
        return hasProperty(obj, key, JSInteropUtil.createKeyInfo());
    }

    /**
     * Sends the "REMOVE" message to obj.
     */
    public static boolean remove(TruffleObject obj, Object key, Node removeNode) {
        try {
            ForeignAccess.sendRemove(removeNode, obj, key);
            return true;
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(obj, e, Message.REMOVE, null);
        }
    }

    @TruffleBoundary
    public static boolean remove(TruffleObject obj, Object key) {
        return remove(obj, key, getRemoveNode());
    }

    @TruffleBoundary
    public static Object call(TruffleObject function, Object[] args) {
        return JSRuntime.importValue(call(function, JSRuntime.exportValueArray(args), JSInteropUtil.createCall()));
    }

    /**
     * Call a foreign function, specified by a TruffleObject. The Caller is responsible for
     * converting arguments and return value.
     */
    public static Object call(TruffleObject function, Object[] args, Node callNode) {
        try {
            return ForeignAccess.sendExecute(callNode, function, args);
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(function, e, JSInteropUtil.EXECUTE, null);
        }
    }

    @TruffleBoundary
    public static Object invoke(TruffleObject receiver, String functionName, Object[] args) {
        return JSRuntime.importValue(invoke(receiver, functionName, JSRuntime.exportValueArray(args), JSInteropUtil.createInvoke()));
    }

    /**
     * Invoke a foreign function, specified by a TruffleObject. The Caller is responsible for
     * converting arguments and return value.
     */
    public static Object invoke(TruffleObject receiver, String functionName, Object[] args, Node callNode) {
        try {
            return ForeignAccess.sendInvoke(callNode, receiver, functionName, args);
        } catch (UnsupportedTypeException | ArityException | UnknownIdentifierException | UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(receiver, e, JSInteropUtil.INVOKE, null);
        }
    }

    @TruffleBoundary
    public static Object construct(TruffleObject target, Object[] args) {
        return JSRuntime.importValue(construct(target, JSRuntime.exportValueArray(args), JSInteropUtil.createNew(), null));
    }

    public static Object construct(TruffleObject target, Object[] args, Node newNode, Node parentNode) {
        try {
            return ForeignAccess.sendNew(newNode, target, args);
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(target, e, JSInteropUtil.NEW, parentNode);
        }
    }

    @TruffleBoundary
    public static Object toPrimitiveOrDefault(TruffleObject obj, Object defaultValue) {
        return toPrimitiveOrDefault(obj, defaultValue, getIsNullNode(), getIsBoxedNode(), getUnboxNode());
    }

    public static Object toPrimitiveOrDefault(TruffleObject obj, Object defaultValue, Node isNullNode, Node isBoxedNode, Node unboxNode) {
        if (ForeignAccess.sendIsNull(isNullNode, obj)) {
            return Null.instance;
        } else if (ForeignAccess.sendIsBoxed(isBoxedNode, obj)) {
            return JSRuntime.importValue(unbox(obj, unboxNode));
        }
        return defaultValue;
    }

    public static Object unbox(TruffleObject obj, Node unboxNode) {
        try {
            return ForeignAccess.sendUnbox(unboxNode, obj);
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(obj, e, Message.UNBOX, null);
        }
    }

    @TruffleBoundary
    public static Object unbox(TruffleObject obj) {
        return JSRuntime.importValue(unbox(obj, getUnboxNode()));
    }

    @TruffleBoundary
    public static boolean isBoxed(TruffleObject obj) {
        return ForeignAccess.sendIsBoxed(getIsBoxedNode(), obj);
    }

    @TruffleBoundary
    public static boolean isNull(TruffleObject obj) {
        return ForeignAccess.sendIsNull(getIsNullNode(), obj);
    }
}
