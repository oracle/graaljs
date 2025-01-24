/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.temporal;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateFunctionBuiltinsFactory.JSTemporalPlainDateCompareNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateFunctionBuiltinsFactory.JSTemporalPlainDateFromNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainDateFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainDateFunctionBuiltins.TemporalPlainDateFunction> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainDateFunctionBuiltins();

    protected TemporalPlainDateFunctionBuiltins() {
        super(JSTemporalPlainDate.CLASS_NAME, TemporalPlainDateFunction.class);
    }

    public enum TemporalPlainDateFunction implements BuiltinEnum<TemporalPlainDateFunction> {
        from(1),
        compare(2);

        private final int length;

        TemporalPlainDateFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainDateFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return JSTemporalPlainDateFromNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case compare:
                return JSTemporalPlainDateCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainDateFromNode extends JSTemporalBuiltinOperation {

        public JSTemporalPlainDateFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainDateObject from(Object item, Object options,
                        @Cached ToTemporalDateNode toTemporalDate) {
            return toTemporalDate.execute(item, options);
        }

    }

    public abstract static class JSTemporalPlainDateCompareNode extends JSTemporalBuiltinOperation {

        public JSTemporalPlainDateCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int compare(Object obj1, Object obj2,
                        @Cached ToTemporalDateNode toTemporalDate) {
            JSTemporalPlainDateObject one = toTemporalDate.execute(obj1);
            JSTemporalPlainDateObject two = toTemporalDate.execute(obj2);
            return TemporalUtil.compareISODate(
                            one.getYear(), one.getMonth(), one.getDay(),
                            two.getYear(), two.getMonth(), two.getDay());
        }
    }

}
