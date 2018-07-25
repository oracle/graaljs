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
package com.oracle.truffle.js.nodes.instrumentation;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;

/**
 * A utility node used by the instrumentation framework to tag a given node with a specific tag. By
 * wrapping nodes with this class, tagging can be performed lazily, rather than at parsing time.
 *
 */
public final class JSTaggedExecutionNode extends JavaScriptNode {

    @Child private JavaScriptNode child;

    private final Class<? extends Tag> expectedTag;
    private final NodeObjectDescriptor descriptor;

    public static JavaScriptNode createFor(JavaScriptNode originalNode, Class<? extends Tag> expectedTag) {
        JavaScriptNode clone = cloneUninitialized(originalNode);
        JavaScriptNode wrapper = new JSTaggedExecutionNode(clone, expectedTag);
        transferSourceSectionAddExpressionTag(originalNode, wrapper);
        return wrapper;
    }

    public static JavaScriptNode createFor(JavaScriptNode originalNode, Class<? extends Tag> expectedTag, NodeObjectDescriptor descriptor) {
        JavaScriptNode clone = cloneUninitialized(originalNode);
        JavaScriptNode wrapper = new JSTaggedExecutionNode(clone, expectedTag, descriptor);
        transferSourceSectionAddExpressionTag(originalNode, wrapper);
        return wrapper;
    }

    public static JavaScriptNode createFor(JavaScriptNode originalNode, JavaScriptNode transferSourcesFrom, Class<? extends Tag> expectedTag) {
        JavaScriptNode clone = cloneUninitialized(originalNode);
        JavaScriptNode wrapper = new JSTaggedExecutionNode(clone, expectedTag);
        transferSourceSectionAddExpressionTag(transferSourcesFrom, wrapper);
        return wrapper;
    }

    private JSTaggedExecutionNode(JavaScriptNode child, Class<? extends Tag> expectedTag) {
        this(child, expectedTag, null);
    }

    public JSTaggedExecutionNode(JavaScriptNode child, Class<? extends Tag> expectedTag, NodeObjectDescriptor descriptor) {
        this.child = child;
        this.descriptor = descriptor;
        this.expectedTag = expectedTag;
    }

    @Override
    public Object getNodeObject() {
        if (descriptor != null) {
            return descriptor;
        }
        return child.getNodeObject();
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == expectedTag) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return child.execute(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new JSTaggedExecutionNode(cloneUninitialized(child), expectedTag);
    }
}
