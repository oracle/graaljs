/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.runtime.JSContext;

public class IteratorCloseWrapperNode extends JavaScriptNode implements ResumableNode {
    @Child private JavaScriptNode block;
    @Child private JavaScriptNode iterator;
    @Child private IteratorCloseNode iteratorClose;

    protected IteratorCloseWrapperNode(JSContext context, JavaScriptNode block, JavaScriptNode iterator) {
        this(block, iterator, IteratorCloseNode.create(context));
    }

    private IteratorCloseWrapperNode(JavaScriptNode block, JavaScriptNode iterator, IteratorCloseNode iteratorClose) {
        this.block = block;
        this.iterator = iterator;
        this.iteratorClose = iteratorClose;
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode block, JavaScriptNode iterator) {
        return new IteratorCloseWrapperNode(context, block, iterator);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            Object result = block.execute(frame);
            iteratorClose.executeVoid((DynamicObject) iterator.execute(frame));
            return result;
        } catch (Throwable e) {
            iteratorClose.executeAbrupt((DynamicObject) iterator.execute(frame));
            throw e;
        }
    }

    @Override
    public Object resume(VirtualFrame frame) {
        try {
            Object result = block.execute(frame);
            iteratorClose.executeVoid((DynamicObject) iterator.execute(frame));
            return result;
        } catch (YieldException e) {
            throw e;
        } catch (Throwable e) {
            iteratorClose.executeAbrupt((DynamicObject) iterator.execute(frame));
            throw e;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new IteratorCloseWrapperNode(cloneUninitialized(block), cloneUninitialized(iterator), cloneUninitialized(iteratorClose));
    }
}
