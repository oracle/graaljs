/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSRecord;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Contains builtins for Record function.
 */
public final class RecordFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<RecordFunctionBuiltins.RecordFunction> {

    public static final JSBuiltinsContainer BUILTINS = new RecordFunctionBuiltins();

    protected RecordFunctionBuiltins() {
        super(JSRecord.CLASS_NAME, RecordFunction.class);
    }

    public enum RecordFunction implements BuiltinEnum<RecordFunction> {
        fromEntries(1),
        isRecord(1);

        private final int length;

        RecordFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, RecordFunction builtinEnum) {
        switch (builtinEnum) {
            case fromEntries:
                return RecordFunctionBuiltinsFactory.RecordFromEntriesNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isRecord:
                return RecordFunctionBuiltinsFactory.RecordIsRecordNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class RecordFromEntriesNode extends JSBuiltinNode {

        private final BranchProfile errorBranch = BranchProfile.create();

        @Child private RequireObjectCoercibleNode requireObjectCoercibleNode = RequireObjectCoercibleNode.create();
        @Child private GetIteratorNode getIteratorNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private ReadElementNode readElementNode;
        @Child private IsObjectNode isObjectNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        public RecordFromEntriesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Record doObject(Object iterable) {
            requireObjectCoercibleNode.executeVoid(iterable);
            Map<String, Object> fields = new TreeMap<>();
            BiConsumer<Object, Object> adder = (key, value) -> {
                if (JSRuntime.isObject(value)) {
                    errorBranch.enter();
                    throw Errors.createTypeError("Records cannot contain objects", this);
                }
                fields.put(JSRuntime.toString(key),value);
            };
            addEntriesFromIterable(iterable, adder);
            return Record.create(fields);
        }

        private void addEntriesFromIterable(Object iterable, BiConsumer<Object, Object> adder) {
            IteratorRecord iterator = getIterator(iterable);
            try {
                while (true) {
                    Object next = iteratorStep(iterator);
                    if (next == Boolean.FALSE) {
                        break;
                    }
                    Object nextItem = iteratorValue((DynamicObject) next);
                    if (!isObject(nextItem)) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorIteratorResultNotObject(nextItem, this);
                    }
                    Object k = get(nextItem, 0);
                    Object v = get(nextItem, 1);
                    adder.accept(k, v);
                }
            } catch (Exception ex) {
                errorBranch.enter();
                iteratorCloseAbrupt(iterator.getIterator());
                throw ex;
            }
        }

        private IteratorRecord getIterator(Object obj) {
            if (getIteratorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorNode = insert(GetIteratorNode.create(getContext()));
            }
            return getIteratorNode.execute(obj);
        }

        private Object iteratorStep(IteratorRecord iterator) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create(getContext()));
            }
            return iteratorStepNode.execute(iterator);
        }

        private Object iteratorValue(DynamicObject obj) {
            if (iteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorValueNode = insert(IteratorValueNode.create(getContext()));
            }
            return iteratorValueNode.execute( obj);
        }

        private void iteratorCloseAbrupt(DynamicObject iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }

        private Object get(Object obj, long idx) {
            if (readElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readElementNode = insert(ReadElementNode.create(getContext()));
            }
            return readElementNode.executeWithTargetAndIndex(obj, idx);
        }

        private boolean isObject(Object obj) {
            if (isObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isObjectNode = insert(IsObjectNode.create());
            }
            return isObjectNode.executeBoolean(obj);
        }
    }

    public abstract static class RecordIsRecordNode extends JSBuiltinNode {

        public RecordIsRecordNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean doRecord(@SuppressWarnings("unused") Record arg) {
            return true;
        }

        @Specialization(guards = "isJSRecord(arg)")
        protected boolean doJSRecord(@SuppressWarnings("unused") Object arg) {
            return true;
        }

        @Fallback
        protected boolean doOther(@SuppressWarnings("unused") Object arg) {
            return false;
        }
    }
}
