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
package com.oracle.truffle.js.runtime.java.adapter;

import java.util.Objects;

import org.graalvm.collections.Pair;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

@MessageResolution(receiverType = JavaSuperAdapter.class)
public final class JavaSuperAdapter implements TruffleObject {
    private final Object adapter;

    JavaSuperAdapter(Object adapter) {
        this.adapter = Objects.requireNonNull(adapter);
        assert !(adapter instanceof JavaSuperAdapter);
    }

    public Object getAdapter() {
        return adapter;
    }

    static boolean isInstance(TruffleObject object) {
        return object instanceof JavaSuperAdapter;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return JavaSuperAdapterForeign.ACCESS;
    }

    @Resolve(message = "READ")
    abstract static class ReadNode extends Node {
        @Child private Node readNode = Message.READ.createNode();
        @CompilationFinal private Pair<String, String> cachedNameToSuper;

        Object access(JavaSuperAdapter superAdapter, String name) {
            String superMethodName;
            if (cachedNameToSuper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedNameToSuper = Pair.create(name, JavaAdapterFactory.getSuperMethodName(name));
            }
            String cachedName = cachedNameToSuper.getLeft();
            if (cachedName != null) {
                if (cachedName.equals(name)) {
                    superMethodName = cachedNameToSuper.getRight();
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    cachedNameToSuper = Pair.empty();
                    superMethodName = JavaAdapterFactory.getSuperMethodName(name);
                }
            } else {
                superMethodName = JavaAdapterFactory.getSuperMethodName(name);
            }
            try {
                return ForeignAccess.sendRead(readNode, (TruffleObject) superAdapter.getAdapter(), superMethodName);
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw e.raise();
            }
        }
    }

    @Resolve(message = "INVOKE")
    abstract static class InvokeNode extends Node {
        @Child private Node invokeNode = JSInteropUtil.INVOKE.createNode();
        @CompilationFinal private Pair<String, String> cachedNameToSuper;

        Object access(JavaSuperAdapter superAdapter, String name, Object[] args) {
            String superMethodName;
            if (cachedNameToSuper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedNameToSuper = Pair.create(name, JavaAdapterFactory.getSuperMethodName(name));
            }
            String cachedName = cachedNameToSuper.getLeft();
            if (cachedName != null) {
                if (cachedName.equals(name)) {
                    superMethodName = cachedNameToSuper.getRight();
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    cachedNameToSuper = Pair.empty();
                    superMethodName = JavaAdapterFactory.getSuperMethodName(name);
                }
            } else {
                superMethodName = JavaAdapterFactory.getSuperMethodName(name);
            }
            try {
                return ForeignAccess.sendInvoke(invokeNode, (TruffleObject) superAdapter.getAdapter(), superMethodName, args);
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                throw e.raise();
            }
        }
    }
}
