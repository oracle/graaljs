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
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.ErrorStackTraceLimitNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.JSObject;

import java.util.Set;

/**
 * 12.13 The throw Statement.
 */
@NodeInfo(shortName = "throw")
public class ThrowNode extends StatementNode {
    @Child private JavaScriptNode exceptionNode;
    @Child private PropertyGetNode getErrorNode;
    @Child private InteropLibrary interopNode;
    @Child private ErrorStackTraceLimitNode stackTraceLimitNode;
    private final JSContext context;

    private final ConditionProfile isError = ConditionProfile.createBinaryProfile();

    protected ThrowNode(JavaScriptNode exceptionNode, JSContext context) {
        this.exceptionNode = exceptionNode;
        this.context = context;
    }

    public static ThrowNode create(JavaScriptNode exceptionNode, JSContext context) {
        return new ThrowNode(exceptionNode, context);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ControlFlowBranchTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("type", ControlFlowBranchTag.Type.Throw.name());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object exceptionObject = exceptionNode.execute(frame);
        if (isError.profile(JSError.isJSError(exceptionObject))) {
            DynamicObject jsobject = (DynamicObject) exceptionObject;
            if (context.isOptionNashornCompatibilityMode()) {
                setLineAndColumnNumber(jsobject);
            }
            throw getException(jsobject);
        } else {
            tryRethrowInterop(exceptionObject);
        }
        throw UserScriptException.create(exceptionObject, this, stackTraceLimitNode().executeInt());
    }

    private void tryRethrowInterop(Object exceptionObject) {
        InteropLibrary interop = interopNode;
        if (interop == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            interopNode = interop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
        }
        if (interop.isException(exceptionObject)) {
            try {
                interop.throwException(exceptionObject);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(exceptionObject, e, "throwException", this);
            }
        }
    }

    private GraalJSException getException(DynamicObject errorObj) {
        if (getErrorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getErrorNode = insert(PropertyGetNode.create(JSError.EXCEPTION_PROPERTY_NAME, JSObject.getJSContext(errorObj)));
        }
        Object exception = getErrorNode.getValue(errorObj);
        return (GraalJSException) exception;

    }

    private ErrorStackTraceLimitNode stackTraceLimitNode() {
        ErrorStackTraceLimitNode node = stackTraceLimitNode;
        if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackTraceLimitNode = node = insert(ErrorStackTraceLimitNode.create());
        }
        return node;
    }

    @TruffleBoundary
    private void setLineAndColumnNumber(DynamicObject jsobject) {
        if (hasSourceSection()) {
            SourceSection sourceSection = getSourceSection();
            JSError.setLineNumber(context, jsobject, sourceSection.getStartLine());
            JSError.setColumnNumber(context, jsobject, sourceSection.getStartColumn());
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(exceptionNode, materializedTags), context);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return true;
    }
}
