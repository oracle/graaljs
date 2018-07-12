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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltins.RegExpPrototypeSymbolOperation;
import com.oracle.truffle.js.builtins.RegExpStringIteratorPrototypeBuiltinsFactory.RegExpStringIteratorNextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %RegExpStringIteratorPrototype% object.
 */
public final class RegExpStringIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<RegExpStringIteratorPrototypeBuiltins.RegExpStringIteratorPrototype> {
    protected RegExpStringIteratorPrototypeBuiltins() {
        super(JSString.REGEXP_ITERATOR_PROTOTYPE_NAME, RegExpStringIteratorPrototype.class);
    }

    public enum RegExpStringIteratorPrototype implements BuiltinEnum<RegExpStringIteratorPrototype> {
        next(0);

        private final int length;

        RegExpStringIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, RegExpStringIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return RegExpStringIteratorNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class RegExpStringIteratorNextNode extends RegExpPrototypeSymbolOperation {

        @Child private HasHiddenKeyCacheNode isRegExpStringIteratorNode;

        @Child private PropertyGetNode getIteratingRegExpNode;
        @Child private PropertyGetNode getIteratedStringNode;
        @Child private PropertyGetNode getGlobalNode;
        @Child private PropertyGetNode getUnicodeNode;
        @Child private PropertyGetNode getDoneNode;

        @Child private PropertySetNode setDoneNode;

        @Child private JSToStringNode toStringNode;
        @Child private JSToLengthNode toLengthNode;

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        private final ConditionProfile noMatchProfile = ConditionProfile.createCountingProfile();
        private final ConditionProfile globalProfile = ConditionProfile.createBinaryProfile();

        public RegExpStringIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isRegExpStringIteratorNode = insert(HasHiddenKeyCacheNode.create(JSString.REGEXP_ITERATOR_ITERATING_REGEXP_ID));
        }

        @Specialization(guards = "isRegExpStringIterator(iterator)")
        protected DynamicObject doRegExpStringIterator(VirtualFrame frame, DynamicObject iterator) {

            boolean done;
            try {
                done = getGetDoneNode().getValueBoolean(iterator);
            } catch (UnexpectedResultException e) {
                throw Errors.shouldNotReachHere();
            }
            if (done) {
                return getCreateIterResultObjectNode().execute(frame, Undefined.instance, true);
            }

            Object regex = getGetIteratingRegExpNode().getValue(iterator);
            String string = (String) getGetIteratedStringNode().getValue(iterator);
            boolean global;
            boolean fullUnicode;
            try {
                global = getGetGlobalNode().getValueBoolean(iterator);
                fullUnicode = getGetUnicodeNode().getValueBoolean(iterator);
            } catch (UnexpectedResultException e) {
                throw Errors.shouldNotReachHere();
            }

            // JSRegExpExecIntlNode supports DynamicObjects only
            Object match = getRegexExecIntlNode().execute((DynamicObject) regex, string);

            if (noMatchProfile.profile(match == Null.instance)) {
                getSetDoneNode().setValueBoolean(iterator, true);
                return getCreateIterResultObjectNode().execute(frame, Undefined.instance, true);
            } else {
                if (globalProfile.profile(global)) {
                    String matchStr = getToStringNode().executeString(read(match, 0));
                    if (matchStr.isEmpty()) {
                        int thisIndex = (int) getToLengthNode().executeLong(getGetLastIndexNode().getValue(regex));
                        int nextIndex = fullUnicode ? advanceStringIndexUnicode(string, thisIndex) : thisIndex + 1;
                        getSetLastIndexNode().setValue(regex, nextIndex);
                    }
                    return getCreateIterResultObjectNode().execute(frame, match, false);
                } else {
                    getSetDoneNode().setValueBoolean(iterator, true);
                    return getCreateIterResultObjectNode().execute(frame, match, false);
                }
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected DynamicObject doIncompatibleReceiver(Object iterator) {
            throw Errors.createTypeError("not a RegExp String Iterator");
        }

        protected final boolean isRegExpStringIterator(Object thisObj) {
            // If the [[IteratingRegExp]] internal slot is present, the others must be as well.
            return isRegExpStringIteratorNode.executeHasHiddenKey(thisObj);
        }

        private PropertyGetNode getGetIteratingRegExpNode() {
            if (getIteratingRegExpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratingRegExpNode = insert(PropertyGetNode.createGetHidden(JSString.REGEXP_ITERATOR_ITERATING_REGEXP_ID, getContext()));
            }
            return getIteratingRegExpNode;
        }

        private PropertyGetNode getGetIteratedStringNode() {
            if (getIteratedStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratedStringNode = insert(PropertyGetNode.createGetHidden(JSString.REGEXP_ITERATOR_ITERATED_STRING_ID, getContext()));
            }
            return getIteratedStringNode;
        }

        private PropertyGetNode getGetGlobalNode() {
            if (getGlobalNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getGlobalNode = insert(PropertyGetNode.createGetHidden(JSString.REGEXP_ITERATOR_GLOBAL_ID, getContext()));
            }
            return getGlobalNode;
        }

        private PropertyGetNode getGetUnicodeNode() {
            if (getUnicodeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getUnicodeNode = insert(PropertyGetNode.createGetHidden(JSString.REGEXP_ITERATOR_UNICODE_ID, getContext()));
            }
            return getUnicodeNode;
        }

        private PropertyGetNode getGetDoneNode() {
            if (getDoneNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDoneNode = insert(PropertyGetNode.createGetHidden(JSString.REGEXP_ITERATOR_DONE_ID, getContext()));
            }
            return getDoneNode;
        }

        private PropertySetNode getSetDoneNode() {
            if (setDoneNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setDoneNode = insert(PropertySetNode.createSetHidden(JSString.REGEXP_ITERATOR_DONE_ID, getContext()));
            }
            return setDoneNode;
        }

        private JSToStringNode getToStringNode() {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode;
        }

        private JSToLengthNode getToLengthNode() {
            if (toLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toLengthNode = insert(JSToLengthNode.create());
            }
            return toLengthNode;
        }

        private CreateIterResultObjectNode getCreateIterResultObjectNode() {
            if (createIterResultObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createIterResultObjectNode = insert(CreateIterResultObjectNode.create(getContext()));
            }
            return createIterResultObjectNode;
        }
    }
}
