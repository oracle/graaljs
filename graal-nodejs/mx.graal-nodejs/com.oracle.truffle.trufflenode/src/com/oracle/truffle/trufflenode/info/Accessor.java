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
package com.oracle.truffle.trufflenode.info;

import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.trufflenode.ContextData;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.node.ExecuteNativeAccessorNode;
import java.util.concurrent.atomic.AtomicInteger;

public class Accessor {

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private final GraalJSAccess graalAccess;
    private final String name;
    private final long getterPtr;
    private final long setterPtr;
    private final Object data;
    private final FunctionTemplate signature;
    private final int id;
    private final int attributes;

    public Accessor(GraalJSAccess graalAccess, Object name, long getterPtr, long setterPtr, Object data, FunctionTemplate signature, int attributes) {
        this.graalAccess = graalAccess;
        this.name = (String) name;
        this.getterPtr = getterPtr;
        this.setterPtr = setterPtr;
        this.data = data;
        this.signature = signature;
        this.id = idGenerator.getAndIncrement();
        this.attributes = attributes;
    }

    public long getGetterPtr() {
        return getterPtr;
    }

    public long getSetterPtr() {
        return setterPtr;
    }

    public String getName() {
        return name;
    }

    public Object getData() {
        return data;
    }

    public FunctionTemplate getSignature() {
        return signature;
    }

    public int getAttributes() {
        return attributes;
    }

    private Pair<JSFunctionData, JSFunctionData> createFunctions(JSContext context) {
        JSFunctionData getter = (getterPtr == 0) ? null : createFunction(context, true);
        JSFunctionData setter = createFunction(context, false);
        return new Pair<>(getter, setter);
    }

    private JSFunctionData createFunction(JSContext context, boolean getter) {
        return GraalJSAccess.functionDataFromRootNode(context, new ExecuteNativeAccessorNode(graalAccess, context, this, getter));
    }

    public Pair<JSFunctionData, JSFunctionData> getFunctions(JSContext context) {
        ContextData contextData = GraalJSAccess.getContextEmbedderData(context);
        Pair<JSFunctionData, JSFunctionData> functions = contextData.getAccessorPair(id);
        if (functions == null) {
            functions = createFunctions(context);
            contextData.setAccessorPair(id, functions);
        }
        return functions;
    }

}
