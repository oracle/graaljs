/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.StringIteratorPrototypeBuiltinsFactory.StringIteratorNextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSStringIterator;
import com.oracle.truffle.js.runtime.builtins.JSStringIteratorObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %StringIteratorPrototype% object.
 */
public final class StringIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<StringIteratorPrototypeBuiltins.StringIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new StringIteratorPrototypeBuiltins();

    protected StringIteratorPrototypeBuiltins() {
        super(JSStringIterator.PROTOTYPE_NAME, StringIteratorPrototype.class);
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
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private TruffleString.ReadCharUTF16Node stringReadNode;
        private final CountingConditionProfile isSurrogatePair = CountingConditionProfile.create();

        public StringIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            this.stringReadNode = TruffleString.ReadCharUTF16Node.create();
        }

        @Specialization
        protected final JSObject doStringIterator(JSStringIteratorObject iterator,
                        @Cached TruffleString.FromCodePointNode fromCodePointNode,
                        @Cached TruffleString.SubstringByteIndexNode substringNode) {
            TruffleString string = iterator.getIteratedString();
            if (string == null) {
                return createIterResultObjectNode.execute(Undefined.instance, true);
            }

            int index = iterator.getNextIndex();
            int length = Strings.length(string);

            if (index >= length) {
                iterator.setIteratedString(null);
                return createIterResultObjectNode.execute(Undefined.instance, true);
            }

            char first = Strings.charAt(stringReadNode, string, index);
            TruffleString result;
            if (isSurrogatePair.profile(Character.isHighSurrogate(first) && index + 1 < length) && Character.isLowSurrogate(Strings.charAt(stringReadNode, string, index + 1))) {
                result = Strings.substring(getContext(), substringNode, string, index, 2);
            } else {
                result = Strings.fromCodePoint(fromCodePointNode, first);
            }
            iterator.setNextIndex(index + Strings.length(result));
            return createIterResultObjectNode.execute(result, false);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static JSObject doIncompatibleReceiver(Object iterator) {
            throw Errors.createTypeError("not a String Iterator");
        }
    }
}
