/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.intl;

import org.graalvm.shadowed.com.ibm.icu.text.BreakIterator;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.SegmentIteratorPrototypeBuiltinsFactory.SegmentIteratorNextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.CreateSegmentDataObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmentIteratorObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenter;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %SegmentIteratorPrototype% object.
 */
public final class SegmentIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<SegmentIteratorPrototypeBuiltins.SegmentIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new SegmentIteratorPrototypeBuiltins();

    protected SegmentIteratorPrototypeBuiltins() {
        super(JSSegmenter.ITERATOR_PROTOTYPE_NAME, SegmentIteratorPrototype.class);
    }

    public enum SegmentIteratorPrototype implements BuiltinEnum<SegmentIteratorPrototype> {
        next(0);

        private final int length;

        SegmentIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SegmentIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return SegmentIteratorNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class SegmentIteratorNextNode extends JSBuiltinNode {

        @Child protected CreateIterResultObjectNode createIterResultObjectNode;

        public SegmentIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization
        protected JSDynamicObject doSegmentIterator(JSSegmentIteratorObject iteratorObj,
                        @Cached("create(getContext())") CreateSegmentDataObjectNode createNextValueNode) {
            JSSegmenter.IteratorState iterator = iteratorObj.getIteratorState();
            TruffleString iteratedString = iterator.getIteratedString();
            BreakIterator icuIterator = iterator.getBreakIterator();
            JSSegmenter.Granularity segmenterGranularity = iterator.getSegmenterGranularity();
            int startIndex = findBoundaryCurrent(icuIterator);
            int endIndex = findBoundaryNext(icuIterator);
            boolean done = endIndex == BreakIterator.DONE;

            Object nextValue;
            if (done) {
                nextValue = Undefined.instance;
            } else {
                nextValue = createNextValueNode.execute(icuIterator, segmenterGranularity, iteratedString, startIndex, endIndex);
            }

            return createIterResultObjectNode.execute(nextValue, done);
        }

        @Specialization(guards = "!isJSSegmentIterator(iterator)")
        protected JSDynamicObject doIncompatibleReceiver(@SuppressWarnings("unused") Object iterator) {
            throw Errors.createTypeErrorTypeXExpected(JSSegmenter.ITERATOR_CLASS_NAME);
        }

        @TruffleBoundary
        private static int findBoundaryCurrent(BreakIterator breakIterator) {
            return breakIterator.current();
        }

        @TruffleBoundary
        private static int findBoundaryNext(BreakIterator breakIterator) {
            return breakIterator.next();
        }

    }

}
