/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.trufflenode.ContextData;
import com.oracle.truffle.trufflenode.EngineCacheData;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.node.ExecuteNativeAccessorNode;

/**
 * Represents an object accessor. Initialized at Node.js initialization time, usually from native.
 * Contains runtime data (e.g. pointers), and must not be persisted.
 */
public class Accessor {

    private final Object name;
    private final long getterPtr;
    private final long setterPtr;
    private final Object data;
    private final int id;
    private final int attributes;

    public Accessor(int accessorId, Object name, long getterPtr, long setterPtr, Object data, int attributes) {
        this.name = name;
        this.getterPtr = getterPtr;
        this.setterPtr = setterPtr;
        this.data = data;
        this.id = accessorId;
        this.attributes = attributes;
    }

    public long getGetterPtr() {
        return getterPtr;
    }

    public long getSetterPtr() {
        return setterPtr;
    }

    public Object getName() {
        return name;
    }

    public Object getData() {
        return data;
    }

    public int getAttributes() {
        return attributes;
    }

    private Pair<JSFunctionData, JSFunctionData> createFunctions(JSContext context) {
        JSFunctionData getter = (getterPtr == 0) ? null : createFunction(context, true);
        JSFunctionData setter = JSAttributes.isWritable(attributes) ? createFunction(context, false) : null;
        return new Pair<>(getter, setter);
    }

    private JSFunctionData createFunction(JSContext context, boolean getter) {
        EngineCacheData cacheData = GraalJSAccess.getContextEngineCacheData(context);
        return cacheData.getOrCreateFunctionDataFromAccessor(this, getter, (c) -> {
            RootNode rootNode = new ExecuteNativeAccessorNode(context, getter);
            CallTarget callbackCallTarget = rootNode.getCallTarget();
            return JSFunctionData.create(context, callbackCallTarget, callbackCallTarget, 0, Strings.EMPTY_STRING, false, false, false, true);
        });
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

    public Descriptor getEngineCacheDescriptor(boolean getter) {
        return new Descriptor(this, getter);
    }

    public static class Descriptor {

        private final Object name;
        private final int attributes;
        private final boolean getter;

        public Descriptor(Accessor accessor, boolean getter) {
            this.name = accessor.name;
            this.attributes = accessor.attributes;
            this.getter = getter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Descriptor that = (Descriptor) o;
            return Objects.equals(this.name, that.name) &&
                            this.attributes == that.attributes &&
                            this.getter == that.getter;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name,
                            this.attributes,
                            this.getter);
        }
    }
}
