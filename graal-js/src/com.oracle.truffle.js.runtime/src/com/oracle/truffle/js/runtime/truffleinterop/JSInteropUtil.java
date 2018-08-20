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

import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.Node;

/**
 * Utility class for interop operations. Provides methods that can be used in Cached annotations of
 * the TruffleDSL to create interop nodes just for specific specializations.
 *
 */
public final class JSInteropUtil {
    private JSInteropUtil() {
        // this class should not be instantiated
    }

    public static Node createHasSize() {
        return Message.HAS_SIZE.createNode();
    }

    public static Node createGetSize() {
        return Message.GET_SIZE.createNode();
    }

    public static Node createRead() {
        return Message.READ.createNode();
    }

    public static Node createWrite() {
        return Message.WRITE.createNode();
    }

    public static Node createHasKeys() {
        return Message.HAS_KEYS.createNode();
    }

    public static Node createKeys() {
        return Message.KEYS.createNode();
    }

    public static Node createIsBoxed() {
        return Message.IS_BOXED.createNode();
    }

    public static Node createUnbox() {
        return Message.UNBOX.createNode();
    }

    public static Node createIsNull() {
        return Message.IS_NULL.createNode();
    }

    public static Node createCall() {
        return Message.EXECUTE.createNode();
    }

    public static Node createInvoke() {
        return Message.INVOKE.createNode();
    }

    public static Node createNew() {
        return Message.NEW.createNode();
    }

    public static Node createRemove() {
        return Message.REMOVE.createNode();
    }

    public static Node createIsExecutable() {
        return Message.IS_EXECUTABLE.createNode();
    }

    public static Node createIsInstantiable() {
        return Message.IS_INSTANTIABLE.createNode();
    }

    public static Node createKeyInfo() {
        return Message.KEY_INFO.createNode();
    }
}
