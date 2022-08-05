/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.InternalCallNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class IteratorHelperPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorHelperPrototypeBuiltins();

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("IteratorHelper.prototype");

    public static final TruffleString CLASS_NAME = Strings.constant("IteratorHelper");

    public static final TruffleString TO_STRING_TAG = Strings.constant("Iterator Helper");


    public static final HiddenKey NEXT_ID = new HiddenKey("next");
    public static final HiddenKey ARGS_ID = new HiddenKey("target");


    protected IteratorHelperPrototypeBuiltins() {
        super(JSArray.ITERATOR_PROTOTYPE_NAME, IteratorHelperPrototypeBuiltins.HelperIteratorPrototype.class);
    }

    public enum HelperIteratorPrototype implements BuiltinEnum<IteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {
        next(1),
        return_(0);

        private final int length;

        HelperIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorHelperPrototypeBuiltins.HelperIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case return_:
                return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class IteratorHelperReturnNode extends JSBuiltinNode {
        @Child private PropertyGetNode getTargetNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        protected IteratorHelperReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = PropertyGetNode.create(ARGS_ID, context);
            iteratorCloseNode = IteratorCloseNode.create(context);
            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object close(VirtualFrame frame, JSObject thisObj) {
            IteratorRecord iterated = ((IteratorPrototypeBuiltins.IteratorArgs) getTargetNode.getValue(thisObj)).target;

            iteratorCloseNode.executeVoid(iterated.getIterator());
            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
        }
    }

    public abstract static class IteratorHelperNextNode extends JSBuiltinNode {
        @Child private PropertyGetNode getNextImplNode;
        @Child private HasHiddenKeyCacheNode hasNextImplNode;
        @Child private InternalCallNode callNode;

        protected IteratorHelperNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getNextImplNode = PropertyGetNode.createGetHidden(NEXT_ID, context);
            hasNextImplNode = HasHiddenKeyCacheNode.create(NEXT_ID);
            callNode = InternalCallNode.create();
        }

        protected boolean hasImpl(Object thisObj) {
            return hasNextImplNode.executeHasHiddenKey(thisObj);
        }

        @Specialization(guards = "hasImpl(thisObj)")
        public Object next(VirtualFrame frame, Object thisObj) {
            return callNode.execute((CallTarget) getNextImplNode.getValue(thisObj), frame.getArguments());
        }

        @Specialization(guards = "!hasImpl(thisObj)")
        public Object unsupported(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }
    }
}
