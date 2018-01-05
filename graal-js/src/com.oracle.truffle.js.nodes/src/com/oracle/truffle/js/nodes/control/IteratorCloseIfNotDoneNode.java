/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.runtime.JSContext;

public class IteratorCloseIfNotDoneNode extends JavaScriptNode implements ResumableNode {
    @Child private JavaScriptNode block;
    @Child private JavaScriptNode iterator;
    @Child private IteratorCloseNode iteratorClose;
    @Child private JavaScriptNode done;

    protected IteratorCloseIfNotDoneNode(JSContext context, JavaScriptNode block, JavaScriptNode iterator, JavaScriptNode done) {
        this(block, iterator, done, IteratorCloseNode.create(context));
    }

    private IteratorCloseIfNotDoneNode(JavaScriptNode block, JavaScriptNode iterator, JavaScriptNode done, IteratorCloseNode iteratorClose) {
        this.block = block;
        this.iterator = iterator;
        this.done = done;
        this.iteratorClose = iteratorClose;
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode block, JavaScriptNode iterator, JavaScriptNode done) {
        return new IteratorCloseIfNotDoneNode(context, block, iterator, done);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result;
        try {
            result = block.execute(frame);
        } catch (YieldException e) {
            throw e;
        } catch (ControlFlowException e) {
            Object iteratorValue = iterator.execute(frame);
            Object isDone = done.execute(frame);
            if (isDone != Boolean.TRUE) {
                iteratorClose.executeVoid((DynamicObject) iteratorValue);
            }
            throw e;
        } catch (Throwable e) {
            Object iteratorValue = iterator.execute(frame);
            Object isDone = done.execute(frame);
            if (isDone != Boolean.TRUE) {
                iteratorClose.executeAbrupt((DynamicObject) iteratorValue);
            }
            throw e;
        }

        Object iteratorValue = iterator.execute(frame);
        Object isDone = done.execute(frame);
        if (isDone != Boolean.TRUE) {
            iteratorClose.executeVoid((DynamicObject) iteratorValue);
        }
        return result;
    }

    @Override
    public Object resume(VirtualFrame frame) {
        Object result;
        try {
            result = block.execute(frame);
        } catch (YieldException e) {
            throw e;
        } catch (ControlFlowException e) {
            Object iteratorValue = iterator.execute(frame);
            Object isDone = done.execute(frame);
            if (isDone != Boolean.TRUE) {
                iteratorClose.executeVoid((DynamicObject) iteratorValue);
            }
            throw e;
        } catch (Throwable e) {
            Object iteratorValue = iterator.execute(frame);
            Object isDone = done.execute(frame);
            if (isDone != Boolean.TRUE) {
                iteratorClose.executeAbrupt((DynamicObject) iteratorValue);
            }
            throw e;
        }
        Object iteratorValue = iterator.execute(frame);
        Object isDone = done.execute(frame);
        if (isDone != Boolean.TRUE) {
            iteratorClose.executeVoid((DynamicObject) iteratorValue);
        }
        return result;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new IteratorCloseIfNotDoneNode(cloneUninitialized(block), cloneUninitialized(iterator), cloneUninitialized(done), cloneUninitialized(iteratorClose));
    }
}
