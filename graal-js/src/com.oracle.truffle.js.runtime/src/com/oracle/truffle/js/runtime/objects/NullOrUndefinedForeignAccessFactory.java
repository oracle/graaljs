/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;

final class NullOrUndefinedForeignAccessFactory implements ForeignAccess.Factory, ForeignAccess.StandardFactory {

    private static final ForeignAccess FOREIGN_ACCESS = ForeignAccess.create(null, new NullOrUndefinedForeignAccessFactory());

    private NullOrUndefinedForeignAccessFactory() {
    }

    static ForeignAccess getForeignAccess() {
        return FOREIGN_ACCESS;
    }

    @Override
    public boolean canHandle(TruffleObject o) {
        return o == Null.instance || o == Undefined.instance;
    }

    @Override
    public CallTarget accessMessage(Message tree) {
        return null;
    }

    @Override
    public CallTarget accessRead() {
        return createCallTarget(new UnsupportedMessageNode(Message.READ));
    }

    @Override
    public CallTarget accessWrite() {
        return createCallTarget(new UnsupportedMessageNode(Message.WRITE));
    }

    @Override
    public CallTarget accessExecute(int argumentsLength) {
        return createCallTarget(new TypeErrorNode(InteropEvent.EXECUTE));
    }

    @Override
    public CallTarget accessInvoke(int argumentsLength) {
        return createCallTarget(new TypeErrorNode(InteropEvent.INVOKE));
    }

    @Override
    public CallTarget accessIsExecutable() {
        return createCallTarget(new ValueNode(false));
    }

    @Override
    public CallTarget accessIsNull() {
        return createCallTarget(new ValueNode(true));
    }

    @Override
    public CallTarget accessHasSize() {
        return createCallTarget(new ValueNode(false));
    }

    @Override
    public CallTarget accessGetSize() {
        return createCallTarget(new ValueNode(0));
    }

    @Override
    public CallTarget accessIsBoxed() {
        return createCallTarget(new ValueNode(false));
    }

    @Override
    public CallTarget accessUnbox() {
        return null;
    }

    @Override
    public CallTarget accessNew(int argumentsLength) {
        return createCallTarget(new TypeErrorNode(InteropEvent.NEW));
    }

    @Override
    public CallTarget accessKeys() {
        return createCallTarget(new UnsupportedMessageNode(Message.KEYS));
    }

    @Override
    public CallTarget accessKeyInfo() {
        return createCallTarget(new UnsupportedMessageNode(Message.KEY_INFO));
    }

    private static CallTarget createCallTarget(RootNode node) {
        return Truffle.getRuntime().createCallTarget(node);
    }

    private static class TypeErrorNode extends RootNode {
        private final InteropEvent event;

        TypeErrorNode(InteropEvent event) {
            super(null, null);
            this.event = event;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.createTypeError(getErrorMessage(frame, event));
        }
    }

    private static class UnsupportedMessageNode extends RootNode {
        private final Message message;

        UnsupportedMessageNode(Message message) {
            super(null, null);
            this.message = message;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw UnsupportedMessageException.raise(message);
        }
    }

    private static class ValueNode extends RootNode {
        private final Object value;

        ValueNode(Object value) {
            super(null, null);
            this.value = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return value;
        }
    }

    private static String getErrorMessage(VirtualFrame frame, InteropEvent event) {
        Object receiver = ForeignAccess.getReceiver(frame);
        assert receiver == Null.instance || receiver == Undefined.instance;
        String receiverName = JSObject.defaultToString((DynamicObject) receiver);
        return getErrorMessageIntl(event, receiverName);
    }

    @TruffleBoundary
    private static String getErrorMessageIntl(InteropEvent event, String receiverName) {
        switch (event) {
            case READ:
                return "Cannot read properties of " + receiverName;
            case WRITE:
                return "Cannot write properties of " + receiverName;
            case KEYS:
                return "Cannot get property keys of " + receiverName;
            case NEW:
            case EXECUTE:
            case INVOKE:
                return receiverName + " is not a function";
            default:
                return "";
        }
    }

    private enum InteropEvent {
        READ,
        WRITE,
        EXECUTE,
        INVOKE,
        NEW,
        KEYS,
    }
}
