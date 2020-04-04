/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;
import java.util.Set;

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
    private final boolean inputTag;
    private final NodeObjectDescriptor descriptor;

    public static JavaScriptNode createFor(JavaScriptNode originalNode, Class<? extends Tag> expectedTag, Set<Class<? extends Tag>> materializedTags) {
        return createImpl(originalNode, originalNode, expectedTag, false, null, materializedTags);
    }

    public static JavaScriptNode createForInput(JavaScriptNode originalNode, Class<? extends Tag> expectedTag, Set<Class<? extends Tag>> materializedTags) {
        return createImpl(originalNode, originalNode, expectedTag, true, null, materializedTags);
    }

    public static JavaScriptNode createForInput(JavaScriptNode originalNode, Class<? extends Tag> expectedTag, NodeObjectDescriptor descriptor, Set<Class<? extends Tag>> materializedTags) {
        return createImpl(originalNode, originalNode, expectedTag, true, descriptor, materializedTags);
    }

    public static JavaScriptNode createForInput(JavaScriptNode originalNode, JavaScriptNode transferSourcesFrom, Set<Class<? extends Tag>> materializedTags) {
        return createImpl(originalNode, transferSourcesFrom, null, true, null, materializedTags);
    }

    private static JavaScriptNode createImpl(JavaScriptNode originalNode, JavaScriptNode transferSourcesFrom, Class<? extends Tag> expectedTag, boolean inputTag, NodeObjectDescriptor descriptor,
                    Set<Class<? extends Tag>> materializedTags) {
        // check if the node has already been tagged
        JavaScriptNode realOriginal = originalNode;
        if (originalNode instanceof WrapperNode) {
            realOriginal = (JavaScriptNode) ((WrapperNode) originalNode).getDelegateNode();
        }
        if (realOriginal.hasTag(expectedTag) && (!inputTag || realOriginal.hasTag(JSTags.InputNodeTag.class))) {
            return originalNode;
        }
        JavaScriptNode clone = cloneUninitialized(originalNode, materializedTags);
        JavaScriptNode wrapper = new JSTaggedExecutionNode(clone, expectedTag, inputTag, descriptor);
        transferSourceSection(transferSourcesFrom, wrapper);
        return wrapper;
    }

    private JSTaggedExecutionNode(JavaScriptNode child, Class<? extends Tag> expectedTag, boolean inputTag, NodeObjectDescriptor descriptor) {
        this.child = Objects.requireNonNull(child);
        this.expectedTag = expectedTag;
        this.inputTag = inputTag;
        this.descriptor = descriptor;
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
        if (expectedTag != null && tag == expectedTag) {
            return true;
        } else if (tag == JSTags.InputNodeTag.class) {
            return inputTag;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return child.execute(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new JSTaggedExecutionNode(cloneUninitialized(child, materializedTags), expectedTag, inputTag, descriptor);
    }

    public JavaScriptNode getDelegateNode() {
        return child;
    }
}
