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
package com.oracle.truffle.js.nodes.function;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInterface;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
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

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BuiltinRootTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("name", getBuiltin().getFullName());
    }

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
