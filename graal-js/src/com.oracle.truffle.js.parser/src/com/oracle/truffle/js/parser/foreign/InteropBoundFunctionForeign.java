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
package com.oracle.truffle.js.parser.foreign;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.ForeignAccess.StandardFactory;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.ExecuteSubNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.InvokeSubNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.IsInstantiableSubNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.KeyInfoSubNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.KeysSubNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.NewSubNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.ReadSubNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.RemoveSubNode;
import com.oracle.truffle.js.parser.foreign.JSForeignAccessFactoryForeign.WriteSubNode;
import com.oracle.truffle.js.runtime.truffleinterop.InteropBoundFunction;

public final class InteropBoundFunctionForeign implements StandardFactory {
    public static final ForeignAccess ACCESS = ForeignAccess.create(InteropBoundFunction.class, new InteropBoundFunctionForeign());

    private InteropBoundFunctionForeign() {
    }

    @Override
    public CallTarget accessIsNull() {
        return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(false));
    }

    @Override
    public CallTarget accessIsExecutable() {
        return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(true));
    }

    @Override
    public CallTarget accessIsInstantiable() {
        return Truffle.getRuntime().createCallTarget(IsInstantiableSubNode.createRoot());
    }

    @Override
    public CallTarget accessIsBoxed() {
        return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(false));
    }

    @Override
    public CallTarget accessHasKeys() {
        return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(true));
    }

    @Override
    public CallTarget accessHasSize() {
        return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(false));
    }

    @Override
    public CallTarget accessRead() {
        return Truffle.getRuntime().createCallTarget(ReadSubNode.createRoot());
    }

    @Override
    public CallTarget accessWrite() {
        return Truffle.getRuntime().createCallTarget(WriteSubNode.createRoot());
    }

    @Override
    public CallTarget accessRemove() {
        return Truffle.getRuntime().createCallTarget(RemoveSubNode.createRoot());
    }

    @Override
    public CallTarget accessExecute(int argumentsLength) {
        return Truffle.getRuntime().createCallTarget(ExecuteSubNode.createRoot());
    }

    @Override
    public CallTarget accessInvoke(int argumentsLength) {
        return Truffle.getRuntime().createCallTarget(InvokeSubNode.createRoot());
    }

    @Override
    public CallTarget accessNew(int argumentsLength) {
        return Truffle.getRuntime().createCallTarget(NewSubNode.createRoot());
    }

    @Override
    public CallTarget accessKeyInfo() {
        return Truffle.getRuntime().createCallTarget(KeyInfoSubNode.createRoot());
    }

    @Override
    public CallTarget accessKeys() {
        return Truffle.getRuntime().createCallTarget(KeysSubNode.createRoot());
    }

    @Override
    public CallTarget accessIsPointer() {
        return Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(false));
    }
}
