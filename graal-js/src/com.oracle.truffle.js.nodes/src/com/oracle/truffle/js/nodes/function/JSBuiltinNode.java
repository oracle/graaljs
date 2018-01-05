/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInterface;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

/**
 * Represents the body of a built-in function.
 */
@NodeChild(value = "arguments", type = JavaScriptNode[].class)
public abstract class JSBuiltinNode extends AbstractBodyNode {
    private final JSContext context;
    private final JSBuiltin builtin;
    boolean construct;
    boolean newTarget;

    protected JSBuiltinNode(JSContext context, JSBuiltin builtin) {
        this.context = context;
        this.builtin = builtin;
    }

    protected JSBuiltinNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
        this.context = context;
        this.builtin = builtin;
        this.construct = construct;
        this.newTarget = newTarget;
    }

    public final JSContext getContext() {
        return context;
    }

    public JSBuiltin getBuiltin() {
        return builtin;
    }

    public abstract JavaScriptNode[] getArguments();

    public boolean isInlineable() {
        return this instanceof Inlineable;
    }

    public Inlined tryCreateInlined() {
        if (isInlineable()) {
            return ((Inlineable) this).createInlined();
        } else {
            return null;
        }
    }

    public static JSBuiltinNode createBuiltin(JSContext ctx, JSBuiltin builtin, boolean construct, boolean newTarget) {
        return new LazyBuiltinNode(ctx, builtin, construct, newTarget);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return createBuiltin(context, builtin, construct, newTarget);
    }

    static final class LazyBuiltinNode extends JSBuiltinNode {
        private static final boolean VERIFY_ARGUMENT_COUNT = false;

        LazyBuiltinNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            super(context, builtin, construct, newTarget);
            assert builtin != null;

            if (VERIFY_ARGUMENT_COUNT) {
                verifyArgumentCount();
            }
        }

        private void verifyArgumentCount() {
            assert !JSTruffleOptions.SubstrateVM;
            JSBuiltinNode builtinNode = createBuiltinNode();
            int argumentNodeCount = 0;
            Class<? extends JSBuiltinNode> nodeclass = builtinNode.getClass();
            for (Class<?> superclass = nodeclass; superclass != null; superclass = superclass.getSuperclass()) {
                argumentNodeCount += Arrays.stream(superclass.getDeclaredFields()).filter(f -> f.getAnnotation(Child.class) != null && f.getName().startsWith("arguments")).count();
            }
            int providedArgumentNodeCount = 0;
            for (Class<?> superclass = nodeclass; superclass != null; superclass = superclass.getSuperclass()) {
                providedArgumentNodeCount += Arrays.stream(superclass.getDeclaredFields()).filter(f -> f.getAnnotation(Child.class) != null && f.getName().startsWith("arguments")).filter(f -> {
                    try {
                        f.setAccessible(true);
                        return f.get(builtinNode) != null;
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                }).count();
            }
            assert providedArgumentNodeCount == argumentNodeCount : nodeclass + " provided=" + providedArgumentNodeCount + " required=" + argumentNodeCount;
        }

        @Override
        public JavaScriptNode[] getArguments() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            JSBuiltinNode resolved = materialize();
            return resolved.execute(frame);
        }

        private JSBuiltinNode materialize() {
            CompilerAsserts.neverPartOfCompilation();
            JSBuiltinNode builtinNode = createBuiltinNode();
            JSBuiltinNode resolved = replace(builtinNode, "lazy builtin");
            return resolved;
        }

        private JSBuiltinNode createBuiltinNode() {
            return getBuiltin().createNode(getContext(), construct, newTarget);
        }

        @Override
        public boolean isInlineable() {
            return materialize().isInlineable();
        }

        @Override
        public Inlined tryCreateInlined() {
            return materialize().tryCreateInlined();
        }
    }

    /**
     * Interface for trivial built-in nodes that can be inlined without going through a call.
     *
     * The built-in function must not make any calls and must not throw an error (note that argument
     * conversions and side-effecting accesses could make calls or throw errors); otherwise it could
     * be observed that no actual call was made. The inlined node must check these preconditions to
     * ensure it is safe to do an inline execution or throw {@link RewriteToCallException}.
     */
    public interface Inlineable extends NodeInterface {
        Inlined createInlined();
    }

    public interface Inlined extends NodeInterface {
        Object callInlined(Object[] arguments) throws RewriteToCallException;

        default RuntimeException rewriteToCall() {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw RewriteToCallException.INSTANCE;
        }
    }

    @SuppressWarnings("serial")
    static final class RewriteToCallException extends RuntimeException {
        static final RuntimeException INSTANCE = new RewriteToCallException();

        private RewriteToCallException() {
            super(null, null, true, false);
        }
    }
}
