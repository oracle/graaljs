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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.StringIteratorPrototypeBuiltinsFactory.StringIteratorNextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %StringIteratorPrototype% object.
 */
public final class StringIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<StringIteratorPrototypeBuiltins.StringIteratorPrototype> {
    protected StringIteratorPrototypeBuiltins() {
        super(JSString.ITERATOR_PROTOTYPE_NAME, StringIteratorPrototype.class);
    }

    public enum StringIteratorPrototype implements BuiltinEnum<StringIteratorPrototype> {
        next(0);

        private final int length;

        StringIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, StringIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return StringIteratorNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class StringIteratorNextNode extends JSBuiltinNode {
        @Child private HasHiddenKeyCacheNode isStringIteratorNode;
        @Child private PropertyGetNode getIteratedObjectNode;
        @Child private PropertyGetNode getNextIndexNode;
        @Child private PropertySetNode setNextIndexNode;
        @Child private PropertySetNode setIteratedObjectNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        private final ConditionProfile isSingleChar = ConditionProfile.createCountingProfile();
        private final ConditionProfile isLowSurrogate = ConditionProfile.createCountingProfile();

        public StringIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isStringIteratorNode = HasHiddenKeyCacheNode.create(JSString.ITERATED_STRING_ID);
            this.getIteratedObjectNode = PropertyGetNode.createGetHidden(JSString.ITERATED_STRING_ID, context);
            this.getNextIndexNode = PropertyGetNode.createGetHidden(JSString.STRING_ITERATOR_NEXT_INDEX_ID, context);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSString.ITERATED_STRING_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSString.STRING_ITERATOR_NEXT_INDEX_ID, context);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization(guards = "isStringIterator(iterator)")
        protected DynamicObject doStringIterator(VirtualFrame frame, DynamicObject iterator) {
            Object iteratedString = getIteratedObjectNode.getValue(iterator);
            if (iteratedString == Undefined.instance) {
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            String string = (String) iteratedString;
            int index = getNextIndex(iterator);
            int length = string.length();

            if (index >= length) {
                setIteratedObjectNode.setValue(iterator, Undefined.instance);
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            char first = string.charAt(index);
            String result;
            if (isSingleChar.profile(!Character.isHighSurrogate(first) || index + 1 == length)) {
                result = String.valueOf(first);
            } else {
                char second = string.charAt(index + 1);
                if (isLowSurrogate.profile(Character.isLowSurrogate(second))) {
                    result = new String(new char[]{first, second});
                } else {
                    result = String.valueOf(first);
                }
            }
            setNextIndexNode.setValue(iterator, index + result.length());
            return createIterResultObjectNode.execute(frame, result, false);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected DynamicObject doIncompatibleReceiver(Object iterator) {
            throw Errors.createTypeError("not a String Iterator");
        }

        protected final boolean isStringIterator(Object thisObj) {
            // If the [[IteratedString]] internal slot is present, the others must be as well.
            return isStringIteratorNode.executeHasHiddenKey(thisObj);
        }

        private int getNextIndex(DynamicObject iterator) {
            try {
                return getNextIndexNode.getValueInt(iterator);
            } catch (UnexpectedResultException e) {
                throw Errors.shouldNotReachHere();
            }
        }
    }
}
