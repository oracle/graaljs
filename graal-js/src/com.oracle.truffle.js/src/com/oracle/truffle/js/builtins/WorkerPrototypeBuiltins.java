/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.WorkerPrototypeBuiltinsFactory.WorkerGetMessageNodeGen;
import com.oracle.truffle.js.builtins.WorkerPrototypeBuiltinsFactory.WorkerPostMessageNodeGen;
import com.oracle.truffle.js.builtins.WorkerPrototypeBuiltinsFactory.WorkerTerminateNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSWorker;
import com.oracle.truffle.js.runtime.builtins.JSWorkerObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

/**
 * Contains built-in functions of the {@code %Worker.prototype%}.
 */
public final class WorkerPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WorkerPrototypeBuiltins.WorkerPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new WorkerPrototypeBuiltins();

    protected WorkerPrototypeBuiltins() {
        super(JSWorker.PROTOTYPE_NAME, WorkerPrototype.class);
    }

    public enum WorkerPrototype implements BuiltinEnum<WorkerPrototype> {
        getMessage(0),
        postMessage(0),
        terminate(0),
        terminateAndWait(0);

        private final int length;

        WorkerPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WorkerPrototype builtinEnum) {
        switch (builtinEnum) {
            case getMessage:
                return WorkerGetMessageNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case postMessage:
                return WorkerPostMessageNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case terminate:
            case terminateAndWait:
                return WorkerTerminateNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic(JSWorker.class)
    public abstract static class WorkerPostMessageNode extends JSBuiltinNode {

        public WorkerPostMessageNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object doWorker(JSWorkerObject thisObj, Object message, Object transfer) {
            EconomicSet<JSArrayBufferObject> transferSet;
            if (transfer == Undefined.instance) {
                transferSet = null;
            } else if (transfer instanceof JSArrayObject array) {
                long length = JSAbstractArray.arrayGetLength(array);
                if (length >= Integer.MAX_VALUE) {
                    throw Errors.createRangeErrorInvalidArrayLength(this);
                }
                int lengthInt = (int) length;
                transferSet = EconomicSet.create(Equivalence.IDENTITY, lengthInt);
                for (int i = 0; i < lengthInt; i++) {
                    Object transferable = JSObject.get(array, i);
                    if (transferable instanceof JSArrayBufferObject arrayBuffer) {
                        if (!transferSet.add(arrayBuffer)) {
                            throw Errors.createError("ArrayBuffer occurs in the transfer array more than once");
                        }
                    } else {
                        throw Errors.createError("Transfer array elements must be an ArrayBuffer");
                    }
                }
            } else {
                throw Errors.createError("Transfer list must be an Array or undefined");
            }
            thisObj.getAgent().postInMessage(message, transferSet);
            return Undefined.instance;
        }

        @TruffleBoundary
        @Specialization(guards = "!isJSWorker(thisObj)")
        protected Object invalidReceiver(Object thisObj, @SuppressWarnings("unused") Object message, @SuppressWarnings("unused") Object transfer) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    @ImportStatic(JSWorker.class)
    public abstract static class WorkerGetMessageNode extends JSBuiltinNode {

        public WorkerGetMessageNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doWorker(JSWorkerObject thisObj) {
            return thisObj.getAgent().getOutMessage(getRealm());
        }

        @TruffleBoundary
        @Specialization(guards = "!isJSWorker(thisObj)")
        protected Object invalidReceiver(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    @ImportStatic(JSWorker.class)
    public abstract static class WorkerTerminateNode extends JSBuiltinNode {

        public WorkerTerminateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doWorker(JSWorkerObject thisObj) {
            thisObj.getAgent().terminate();
            return Undefined.instance;
        }

        @TruffleBoundary
        @Specialization(guards = "!isJSWorker(thisObj)")
        protected Object invalidReceiver(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

}
