/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.SegmenterPrototypeBuiltinsFactory.JSSegmenterResolvedOptionsNodeGen;
import com.oracle.truffle.js.builtins.intl.SegmenterPrototypeBuiltinsFactory.JSSegmenterSegmentNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenter;
import com.oracle.truffle.js.runtime.builtins.intl.JSSegmenterObject;

public final class SegmenterPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<SegmenterPrototypeBuiltins.SegmenterPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new SegmenterPrototypeBuiltins();

    protected SegmenterPrototypeBuiltins() {
        super(JSSegmenter.PROTOTYPE_NAME, SegmenterPrototype.class);
    }

    public enum SegmenterPrototype implements BuiltinEnum<SegmenterPrototype> {

        resolvedOptions(0),
        segment(1);

        private final int length;

        SegmenterPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SegmenterPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSSegmenterResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case segment:
                return JSSegmenterSegmentNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSSegmenterResolvedOptionsNode extends JSBuiltinNode {

        public JSSegmenterResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSegmenter(segmenter)")
        public Object doResolvedOptions(DynamicObject segmenter) {
            return JSSegmenter.resolvedOptions(getContext(), getRealm(), segmenter);
        }

        @Specialization(guards = "!isJSSegmenter(bummer)")
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorSegmenterExpected();
        }
    }

    public abstract static class JSSegmenterSegmentNode extends JSBuiltinNode {

        public JSSegmenterSegmentNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doSegmenter(JSSegmenterObject segmenter, Object value,
                        @Cached("create()") JSToStringNode toStringNode) {
            String string = toStringNode.executeString(value);
            return JSSegmenter.createSegments(getContext(), getRealm(), segmenter, string);
        }

        @Specialization(guards = "!isJSSegmenter(bummer)")
        @SuppressWarnings("unused")
        public void doOther(Object bummer, Object value) {
            throw Errors.createTypeErrorSegmenterExpected();
        }
    }

}
