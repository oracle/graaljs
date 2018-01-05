/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
