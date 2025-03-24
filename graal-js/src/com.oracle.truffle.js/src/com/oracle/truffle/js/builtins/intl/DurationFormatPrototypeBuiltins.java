/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.DurationFormatPrototypeBuiltinsFactory.JSDurationFormatFormatNodeGen;
import com.oracle.truffle.js.builtins.intl.DurationFormatPrototypeBuiltinsFactory.JSDurationFormatFormatToPartsNodeGen;
import com.oracle.truffle.js.builtins.intl.DurationFormatPrototypeBuiltinsFactory.JSDurationFormatResolvedOptionsNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationRecordNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.intl.JSDurationFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDurationFormatObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;

public final class DurationFormatPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<DurationFormatPrototypeBuiltins.DurationFormatPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new DurationFormatPrototypeBuiltins();

    protected DurationFormatPrototypeBuiltins() {
        super(JSDurationFormat.PROTOTYPE_NAME, DurationFormatPrototype.class);
    }

    public enum DurationFormatPrototype implements BuiltinEnum<DurationFormatPrototype> {

        resolvedOptions(0),
        format(1),
        formatToParts(1);

        private final int length;

        DurationFormatPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, DurationFormatPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSDurationFormatResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case format:
                return JSDurationFormatFormatNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case formatToParts:
                return JSDurationFormatFormatToPartsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSDurationFormatResolvedOptionsNode extends JSBuiltinNode {

        public JSDurationFormatResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object resolvedOptions(JSDurationFormatObject durationFormat) {
            return JSDurationFormat.resolvedOptions(getContext(), getRealm(), durationFormat);
        }

        @Fallback
        public Object throwTypeError(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorTypeXExpected(JSDurationFormat.CLASS_NAME);
        }
    }

    public abstract static class JSDurationFormatFormatNode extends JSBuiltinNode {

        public JSDurationFormatFormatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString format(JSDurationFormatObject durationFormat, Object duration,
                        @Cached ToTemporalDurationRecordNode toTemporalDurationRecord,
                        @Cached TruffleString.FromJavaStringNode fromJavaString) {
            JSTemporalDurationRecord record = toTemporalDurationRecord.execute(duration);
            String result = JSDurationFormat.format(durationFormat.getInternalState(), record);
            return Strings.fromJavaString(fromJavaString, result);
        }

        @Fallback
        @SuppressWarnings("unused")
        public TruffleString throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorTypeXExpected(JSDurationFormat.CLASS_NAME);
        }
    }

    public abstract static class JSDurationFormatFormatToPartsNode extends JSBuiltinNode {

        public JSDurationFormatFormatToPartsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object formatToParts(JSDurationFormatObject durationFormat, Object duration,
                        @Cached ToTemporalDurationRecordNode toTemporalDurationRecord) {
            JSTemporalDurationRecord record = toTemporalDurationRecord.execute(duration);
            return JSDurationFormat.formatToParts(getContext(), getRealm(), durationFormat.getInternalState(), record);
        }

        @Fallback
        @SuppressWarnings("unused")
        public Object throwTypeError(Object bummer, Object duration) {
            throw Errors.createTypeErrorTypeXExpected(JSDurationFormat.CLASS_NAME);
        }
    }
}
