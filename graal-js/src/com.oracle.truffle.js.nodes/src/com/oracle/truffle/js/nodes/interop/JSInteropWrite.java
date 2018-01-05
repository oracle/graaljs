/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.runtime.JSContext;

public final class JSInteropWrite {

    public static JavaScriptNode create(JSContext context, Message message) {
        return new JSInteropUnresolvedWriteNode(context, message);
    }

    private abstract static class JSInteropAbstractWriteNode extends JavaScriptNode {
        protected final JSContext context;
        protected final Message message;
        @Child protected JavaScriptNode receiver;
        @Child protected JavaScriptNode argument0;
        @Child protected JavaScriptNode argument1;

        JSInteropAbstractWriteNode(JSContext context, Message message) {
            this.context = context;
            this.message = message;
            this.receiver = new JSInteropReceiverNode();
            this.argument0 = new JSInteropArgumentNode(0);
            this.argument1 = new JSInteropArgumentNode(1);
        }
    }

    private static final class JSInteropUnresolvedWriteNode extends JSInteropAbstractWriteNode {

        private JSInteropUnresolvedWriteNode(JSContext context, Message message) {
            super(context, message);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object argument = argument0.execute(frame);
            if (argument instanceof String) {
                return this.replace(new JSInteropPropertyWriteNode(context, message, (String) argument)).execute(frame);
            } else {
                return this.replace(new JSInteropElementWriteNode(context, message)).execute(frame);
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new JSInteropUnresolvedWriteNode(context, message);
        }
    }

    private static final class JSInteropPropertyWriteNode extends JSInteropAbstractWriteNode {
        private final String name;
        @Child private PropertySetNode propertySet;
        @Child private JSForeignToJSTypeNode toJSType;

        private JSInteropPropertyWriteNode(JSContext context, Message message, String name) {
            super(context, message);
            this.name = name;
            this.propertySet = PropertySetNode.create(name, false, context, false);
            this.toJSType = insert(JSForeignToJSTypeNodeGen.create());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(argument0.execute(frame))) {
                // access JS property
                Object foreignValue = argument1.execute(frame);
                Object value = toJSType.executeWithTarget(foreignValue);
                propertySet.setValue(receiver.execute(frame), value);
                return value;
            } else {
                // rewrite to element access
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return this.replace(new JSInteropElementWriteNode(context, message)).execute(frame);
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new JSInteropPropertyWriteNode(context, message, name);
        }
    }

    private static final class JSInteropElementWriteNode extends JSInteropAbstractWriteNode {
        @Child private JavaScriptNode writeElementNode;

        private JSInteropElementWriteNode(JSContext context, Message message) {
            super(context, message);
            JavaScriptNode receiverNode = new JSInteropReceiverNode();
            JavaScriptNode argumentNode = new JSInteropArgumentNode(0);
            JavaScriptNode value = new JSInteropArgumentNode(1, true);
            writeElementNode = NodeFactory.getInstance(context).createWriteElementNode(receiverNode, argumentNode, value, context, false);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return writeElementNode.execute(frame);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new JSInteropElementWriteNode(context, message);
        }
    }

}
