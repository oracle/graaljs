/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

/**
 * ES6 7.4.4 IteratorValue(iterResult).
 */
@ImportStatic({JSConfig.class})
public abstract class IteratorValueNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode iterResultNode;
    @Child private PropertyGetNode getValueNode;

    protected IteratorValueNode(JSContext context, JavaScriptNode iterResultNode) {
        this.iterResultNode = iterResultNode;
        this.getValueNode = PropertyGetNode.create(Strings.VALUE, false, context);
    }

    public static IteratorValueNode create(JSContext context) {
        return create(context, null);
    }

    public static IteratorValueNode create(JSContext context, JavaScriptNode iterResult) {
        return IteratorValueNodeGen.create(context, iterResult);
    }

    @Specialization
    protected Object doIteratorNext(JSDynamicObject iterResult) {
        return getValueNode.getValue(iterResult);
    }

    @Specialization(guards = "isForeignObject(obj)", limit = "InteropLibraryLimit")
    protected Object doForeignObject(Object obj,
                    @CachedLibrary("obj") InteropLibrary interop,
                    @Cached ImportValueNode importValueNode) {
        try {
            return importValueNode.executeWithTarget(interop.readMember(obj, Strings.toJavaString(Strings.VALUE)));
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw Errors.createTypeErrorInteropException(obj, e, Strings.toJavaString(Strings.VALUE), this);
        }
    }

    public abstract Object execute(Object iterResult);

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return IteratorValueNodeGen.create(getValueNode.getContext(), cloneUninitialized(iterResultNode, materializedTags));
    }
}
