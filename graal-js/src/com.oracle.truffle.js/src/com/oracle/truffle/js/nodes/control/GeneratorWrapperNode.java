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

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;

public final class GeneratorWrapperNode extends JavaScriptNode implements RepeatingNode {
    @Child private JavaScriptNode childNode;
    private final int stateSlot;

    private GeneratorWrapperNode(JavaScriptNode childNode, int stateSlot) {
        assert childNode instanceof ResumableNode : childNode;
        this.childNode = childNode;
        this.stateSlot = stateSlot;
    }

    public static JavaScriptNode createWrapper(JavaScriptNode child, int stateSlot) {
        JavaScriptNode wrapper = new GeneratorWrapperNode(child, stateSlot);
        Node realChild = child instanceof WrapperNode ? ((WrapperNode) child).getDelegateNode() : child;
        wrapper.setSourceSection(realChild.getSourceSection());
        return wrapper;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        Node child = childNode instanceof WrapperNode ? ((WrapperNode) childNode).getDelegateNode() : childNode;
        if (child instanceof JavaScriptNode) {
            return ((JavaScriptNode) child).hasTag(tag);
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        Node child = childNode instanceof WrapperNode ? ((WrapperNode) childNode).getDelegateNode() : childNode;
        if (child instanceof JavaScriptNode) {
            return ((JavaScriptNode) child).getNodeObject();
        } else {
            return null;
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Node child = childNode;
        if (child instanceof WrapperNode) {
            child = ((WrapperNode) child).getDelegateNode();
        }
        if (child instanceof ResumableNode) {
            return ((ResumableNode) child).resume(frame, stateSlot);
        } else {
            assert false : child.getClass();
            throw Errors.shouldNotReachHere();
        }
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
        assert childNode instanceof ResumableNode && childNode instanceof RepeatingNode : childNode.getClass();
        return (boolean) execute(frame);
    }

    public JavaScriptNode getResumableNode() {
        return childNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new GeneratorWrapperNode(cloneUninitialized(childNode, materializedTags), stateSlot);
    }
}
