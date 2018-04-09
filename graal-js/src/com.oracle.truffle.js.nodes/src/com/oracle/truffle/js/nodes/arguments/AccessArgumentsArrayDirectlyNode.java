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
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.CallApplyArgumentsNode;
import com.oracle.truffle.js.runtime.JSArguments;

/**
 * This node accesses the arguments array optimistically directly as {@code Object[]}, but can fall
 * back to returning a JS Arguments Object.
 *
 * This node is used to avoid unnecessary allocation of the Arguments Object for the
 * {@code function.apply(this, arguments)} call pattern where it would be immediately discarded.
 *
 * @see CallApplyArgumentsNode
 */
public final class AccessArgumentsArrayDirectlyNode extends JavaScriptNode {
    private final int leadingArgCount;
    private final int trailingArgCount;
    @Child private JavaScriptNode writeArgumentsNode;
    @Child private JavaScriptNode readArgumentsNode;

    @CompilationFinal private volatile boolean directArrayAccess = true;
    private final ConditionProfile initializedProfile = ConditionProfile.createBinaryProfile();

    public AccessArgumentsArrayDirectlyNode(JavaScriptNode writeArgumentsNode, JavaScriptNode readArgumentsNode, int leadingArgCount, int trailingArgCount) {
        this.leadingArgCount = leadingArgCount;
        this.trailingArgCount = trailingArgCount;
        this.writeArgumentsNode = writeArgumentsNode;
        this.readArgumentsNode = readArgumentsNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (directArrayAccess) {
            return asObjectArray(frame);
        } else {
            return asArgumentsObject(frame);
        }
    }

    private Object[] asObjectArray(VirtualFrame frame) {
        return JSArguments.extractUserArguments(frame.getArguments(), leadingArgCount, trailingArgCount);
    }

    /**
     * Reads the arguments object slot and checks whether it is already initialized, otherwise
     * allocates and writes the arguments object, and finally returns the arguments object.
     */
    private Object asArgumentsObject(VirtualFrame frame) {
        Object argumentsArray = readArgumentsNode.execute(frame);
        if (initializedProfile.profile(ArgumentsObjectNode.isInitialized(argumentsArray))) {
            return argumentsArray;
        } else {
            return writeArgumentsNode.execute(frame);
        }
    }

    /**
     * Called when the arguments array can no longer be accessed directly.
     */
    public void replaceWithDefaultArguments() {
        CompilerAsserts.neverPartOfCompilation();
        directArrayAccess = false;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new AccessArgumentsArrayDirectlyNode(cloneUninitialized(writeArgumentsNode), cloneUninitialized(readArgumentsNode), leadingArgCount, trailingArgCount);
    }
}
