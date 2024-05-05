/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode.threading;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.JSExternal;
import com.oracle.truffle.trufflenode.JSExternalObject;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingBuiltinsFactory.DisposeNodeGen;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingBuiltinsFactory.EncodedRefsNodeGen;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingBuiltinsFactory.EnterNodeGen;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingBuiltinsFactory.FreeNodeGen;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingBuiltinsFactory.LeaveNodeGen;

public class SharedMemMessagingBuiltins extends JSBuiltinsContainer.SwitchEnum<SharedMemMessagingBuiltins.API> {

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("SharedMemMessaging.prototype");

    protected SharedMemMessagingBuiltins() {
        super(PROTOTYPE_NAME, API.class);
    }

    public enum API implements BuiltinEnum<API> {
        enter(1),
        leave(0),
        free(0),
        encodedJavaRefs(0),
        dispose(1);

        private final int length;

        API(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, API builtinEnum) {
        switch (builtinEnum) {
            case enter:
                return EnterNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case leave:
                return LeaveNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case free:
                return FreeNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case encodedJavaRefs:
                return EncodedRefsNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case dispose:
                return DisposeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    /**
     * Signals that we are starting to encode an object tree onto a native MessagePortData queue.
     */
    public abstract static class EnterNode extends JSBuiltinNode {

        protected EnterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        public Object enter(SharedMemMessagingBindings.Instance self, JSExternalObject nativeMessagePortData) {
            GraalJSAccess.get(this).setCurrentMessagePortData(nativeMessagePortData);
            return self;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Fallback
        public Object incompatibleReceiver(Object self, @SuppressWarnings("unused") Object external) {
            if (self instanceof SharedMemMessagingBindings.Instance) {
                throw Errors.createTypeErrorTypeXExpected(JSExternal.CLASS_NAME);
            }
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), self);
        }
    }

    /**
     * Returns the number of Java objects that were encoded since `enter()` was last called.
     */
    public abstract static class EncodedRefsNode extends JSBuiltinNode {

        protected EncodedRefsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        boolean encodedJavaRefs(@SuppressWarnings("unused") SharedMemMessagingBindings.Instance self) {
            GraalJSAccess access = GraalJSAccess.get(this);
            assert access.getCurrentMessagePortData() != null;
            return access.getCurrentMessagePortData().encodedJavaRefs();
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Fallback
        final Object incompatibleReceiver(Object self) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), self);
        }
    }

    /**
     * Removes the (last) given number of Java references from the queue.
     */
    public abstract static class FreeNode extends JSBuiltinNode {

        protected FreeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        Object free(SharedMemMessagingBindings.Instance self) {
            GraalJSAccess.get(this).getCurrentMessagePortData().disposeLastMessageRefs();
            return self;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Fallback
        final Object incompatibleReceiver(Object self) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), self);
        }
    }

    /**
     * Signals that we should stop encoding Java references while encoding.
     */
    public abstract static class LeaveNode extends JSBuiltinNode {

        protected LeaveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        Object leave(SharedMemMessagingBindings.Instance self) {
            GraalJSAccess.get(this).unsetCurrentMessagePortData();
            return self;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Fallback
        final Object incompatibleReceiver(Object self) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), self);
        }
    }

    /**
     * Free a MessagePortData object and any message pending in its queue.
     */
    public abstract static class DisposeNode extends JSBuiltinNode {

        protected DisposeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        static Object dispose(SharedMemMessagingBindings.Instance self, JSExternalObject external) {
            SharedMemMessagingManager.disposeReferences(external);
            return self;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Fallback
        final Object incompatibleReceiver(Object self, @SuppressWarnings("unused") Object external) {
            if (self instanceof SharedMemMessagingBindings.Instance) {
                throw Errors.createTypeErrorTypeXExpected(JSExternal.CLASS_NAME);
            }
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), self);
        }
    }

}
