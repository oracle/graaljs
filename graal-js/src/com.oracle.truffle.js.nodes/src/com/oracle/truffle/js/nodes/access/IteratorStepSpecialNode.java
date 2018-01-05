/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * (partially) implements ES6 7.4.5 IteratorStep(iterator).
 *
 * Note that this node returns the value instead of the result, thus is non-standard! For the
 * standard-compliant version, see {@link IteratorStepNode}.
 */
@NodeChild(value = "iterator", type = JavaScriptNode.class)
public abstract class IteratorStepSpecialNode extends JavaScriptNode {
    @Child private PropertyGetNode getNextNode;
    @Child private PropertyGetNode getValueNode;
    @Child private PropertyGetNode getDoneNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;
    @Child private JavaScriptNode doneNode;
    @Child private JSToBooleanNode toBooleanNode;
    private final boolean setDoneOnError;

    protected IteratorStepSpecialNode(JSContext context, JavaScriptNode doneNode, boolean setDoneOnError) {
        this.getNextNode = PropertyGetNode.create(JSRuntime.NEXT, false, context);
        this.getValueNode = PropertyGetNode.create(JSRuntime.VALUE, false, context);
        this.getDoneNode = PropertyGetNode.create(JSRuntime.DONE, false, context);
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();
        this.toBooleanNode = JSToBooleanNode.create();
        this.doneNode = doneNode;
        this.setDoneOnError = setDoneOnError;
    }

    public static IteratorStepSpecialNode create(JSContext context, JavaScriptNode iterator, JavaScriptNode doneNode, boolean setDoneOnError) {
        return IteratorStepSpecialNodeGen.create(context, doneNode, setDoneOnError, iterator);
    }

    @Specialization
    protected Object doIteratorStep(VirtualFrame frame, DynamicObject iterator) {
        Object next;
        Object result;
        try {
            next = getNextNode.getValue(iterator);
            result = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, next));
            if (!isObjectNode.executeBoolean(result)) {
                throw Errors.createNotAnObjectError(this);
            }
        } catch (Exception ex) {
            if (setDoneOnError) {
                doneNode.execute(frame);
            }
            throw ex;
        }

        Object value = getValueNode.getValue(result);
        Object done = toBooleanNode.executeBoolean(getDoneNode.getValue(result));
        return done == Boolean.FALSE ? value : doneNode.execute(frame);
    }

    public abstract Object execute(VirtualFrame frame, DynamicObject iterator);

    abstract JavaScriptNode getIterator();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(getNextNode.getContext(), cloneUninitialized(getIterator()), cloneUninitialized(doneNode), setDoneOnError);
    }
}
