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
package com.oracle.truffle.js.builtins.intl;

import com.ibm.icu.text.BreakIterator;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.SegmentsPrototypeBuiltinsFactory.SegmentsContainingNodeGen;
import com.oracle.truffle.js.builtins.intl.SegmentsPrototypeBuiltinsFactory.SegmentsIteratorNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.CreateSegmentDataObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenter;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenterObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmentsObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class SegmentsPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<SegmentsPrototypeBuiltins.SegmentsPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new SegmentsPrototypeBuiltins();

    protected SegmentsPrototypeBuiltins() {
        super(JSSegmenter.SEGMENTS_PROTOTYPE_NAME, SegmentsPrototype.class);
    }

    public enum SegmentsPrototype implements BuiltinEnum<SegmentsPrototype> {
        containing(1),
        _iterator(0) {
            @Override
            public Object getKey() {
                return Symbol.SYMBOL_ITERATOR;
            }
        };

        private final int length;

        SegmentsPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SegmentsPrototype builtinEnum) {
        switch (builtinEnum) {
            case containing:
                return SegmentsContainingNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case _iterator:
                return SegmentsIteratorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class SegmentsContainingNode extends JSBuiltinNode {

        public SegmentsContainingNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doSegments(JSSegmentsObject segments, Object index,
                        @Cached JSToIntegerAsIntNode toIntegerNode,
                        @Cached("create(getContext())") CreateSegmentDataObjectNode createResultNode) {
            int n = toIntegerNode.executeInt(index);
            String string = segments.getSegmentsString();
            int len = string.length();
            if (n < 0 || n >= len) {
                return Undefined.instance;
            }
            JSSegmenterObject segmenter = segments.getSegmentsSegmenter();
            BreakIterator breakIterator = getBreakIterator(segmenter, string);
            int startIndex = findBoundaryBefore(breakIterator, n);
            int endIndex = findBoundaryAfter(breakIterator, n);
            return createResultNode.execute(breakIterator, JSSegmenter.getGranularity(segmenter), string, startIndex, endIndex);
        }

        @TruffleBoundary
        private static BreakIterator getBreakIterator(JSSegmenterObject segmenter, String string) {
            BreakIterator breakIterator = segmenter.getBreakIterator();
            breakIterator.setText(string);
            return breakIterator;
        }

        @TruffleBoundary
        private static int findBoundaryBefore(BreakIterator breakIterator, int index) {
            return breakIterator.preceding(index + 1);
        }

        @TruffleBoundary
        private static int findBoundaryAfter(BreakIterator breakIterator, int index) {
            return breakIterator.following(index);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSegments(bummer)")
        public void doOther(Object bummer, Object index) {
            throw Errors.createTypeErrorSegmentsExpected();
        }
    }

    public abstract static class SegmentsIteratorNode extends JSBuiltinNode {

        @Child private CreateSegmentIteratorNode createSegmentIteratorNode;

        public SegmentsIteratorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createSegmentIteratorNode = CreateSegmentIteratorNode.create(context);
        }

        @Specialization
        public Object doSegments(JSSegmentsObject segments) {
            return createSegmentIteratorNode.execute(segments.getSegmentsSegmenter(), segments.getSegmentsString());
        }

        @Specialization(guards = "!isJSSegments(bummer)")
        @SuppressWarnings("unused")
        public void doOther(Object bummer) {
            throw Errors.createTypeErrorSegmentsExpected();
        }
    }

    public static class CreateSegmentIteratorNode extends JavaScriptBaseNode {
        private final JSContext context;

        protected CreateSegmentIteratorNode(JSContext context) {
            this.context = context;
        }

        public static CreateSegmentIteratorNode create(JSContext context) {
            return new CreateSegmentIteratorNode(context);
        }

        public DynamicObject execute(DynamicObject segmenter, String value) {
            assert JSSegmenter.isJSSegmenter(segmenter);
            return JSSegmenter.createSegmentIterator(context, getRealm(), segmenter, value);
        }
    }
}
