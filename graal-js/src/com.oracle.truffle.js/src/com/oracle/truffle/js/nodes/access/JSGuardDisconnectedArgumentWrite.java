/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;

public abstract class JSGuardDisconnectedArgumentWrite extends JavaScriptNode implements WriteNode {
    private final int argumentIndex;
    @Child @Executed JavaScriptNode argumentsArrayNode;
    @Child @Executed JavaScriptNode rhsNode;
    @Child private WriteElementNode writeArgumentsElementNode;
    private final TruffleString name;

    JSGuardDisconnectedArgumentWrite(int index, WriteElementNode argumentsArrayAccess, JavaScriptNode argumentsArray, JavaScriptNode rhs, TruffleString name) {
        this.argumentIndex = index;
        this.argumentsArrayNode = argumentsArray;
        this.rhsNode = rhs;
        this.writeArgumentsElementNode = argumentsArrayAccess;
        this.name = name;
    }

    public static JSGuardDisconnectedArgumentWrite create(int index, WriteElementNode argumentsArrayAccess, JavaScriptNode argumentsArray, JavaScriptNode rhs, TruffleString name) {
        return JSGuardDisconnectedArgumentWriteNodeGen.create(index, argumentsArrayAccess, argumentsArray, rhs, name);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == WriteVariableTag.class || tag == StandardTags.WriteVariableTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor("name", name);
        descriptor.addProperty(StandardTags.WriteVariableTag.NAME, name);
        return descriptor;
    }

    @Specialization(guards = "!isArgumentsDisconnected(argumentsArray)")
    public Object doObject(JSArgumentsObject argumentsArray, Object value,
                    @Cached @Shared InlinedConditionProfile unconnected) {
        assert JSArgumentsArray.isJSArgumentsObject(argumentsArray);
        if (unconnected.profile(this, argumentIndex >= JSAbstractArgumentsArray.getConnectedArgumentCount(argumentsArray))) {
            JSAbstractArgumentsArray.disconnectIndex(argumentsArray, argumentIndex, value);
        } else {
            writeArgumentsElementNode.executeWithTargetAndIndexAndValue(argumentsArray, argumentIndex, value);
        }
        return value;
    }

    @Specialization(guards = "isArgumentsDisconnected(argumentsArray)")
    public Object doObjectDisconnected(JSArgumentsObject argumentsArray, Object value,
                    @Cached @Shared InlinedConditionProfile wasDisconnected,
                    @Cached @Shared InlinedConditionProfile unconnected) {
        assert JSArgumentsArray.isJSArgumentsObject(argumentsArray);
        if (wasDisconnected.profile(this, JSAbstractArgumentsArray.wasIndexDisconnected(argumentsArray, argumentIndex))) {
            JSAbstractArgumentsArray.setDisconnectedIndexValue(argumentsArray, argumentIndex, value);
        } else if (unconnected.profile(this, argumentIndex >= JSAbstractArgumentsArray.getConnectedArgumentCount(argumentsArray))) {
            JSAbstractArgumentsArray.disconnectIndex(argumentsArray, argumentIndex, value);
        } else {
            writeArgumentsElementNode.executeWithTargetAndIndexAndValue(argumentsArray, argumentIndex, value);
        }
        return value;
    }

    @Override
    public final void executeWrite(VirtualFrame frame, Object value) {
        executeWrite(frame, argumentsArrayNode.execute(frame), value);
    }

    protected abstract void executeWrite(VirtualFrame frame, Object argumentsArray, Object value);

    @Override
    public JavaScriptNode getRhs() {
        return rhsNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSGuardDisconnectedArgumentWriteNodeGen.create(argumentIndex, cloneUninitialized(writeArgumentsElementNode, materializedTags), cloneUninitialized(argumentsArrayNode, materializedTags),
                        cloneUninitialized(rhsNode, materializedTags), name);
    }
}
