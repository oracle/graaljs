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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.BooleanPrototypeBuiltinsFactory.JSBooleanToStringNodeGen;
import com.oracle.truffle.js.builtins.BooleanPrototypeBuiltinsFactory.JSBooleanValueOfNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;

/**
 * Contains builtins for {@linkplain JSBoolean}.prototype.
 */
public final class BooleanPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<BooleanPrototypeBuiltins.BooleanPrototype> {
    protected BooleanPrototypeBuiltins() {
        super(JSBoolean.PROTOTYPE_NAME, BooleanPrototype.class);
    }

    public enum BooleanPrototype implements BuiltinEnum<BooleanPrototype> {
        toString(0),
        valueOf(0);

        private final int length;

        BooleanPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, BooleanPrototype builtinEnum) {
        switch (builtinEnum) {
            case toString:
                return JSBooleanToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSBooleanValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSBooleanToStringNode extends JSBuiltinNode {

        public JSBooleanToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"!isJSBoolean(thisObj)", "!isBoolean(thisObj)"})
        protected String toString(@SuppressWarnings("unused") Object thisObj) {
            throw JSBoolean.noBooleanError();
        }

        @Specialization(guards = "isJSBoolean(thisObj)")
        protected String toString(DynamicObject thisObj) {
            return String.valueOf(JSBoolean.valueOf(thisObj));
        }

        @Specialization(guards = "isBoolean(thisObj)")
        protected String toStringPrimitive(Object thisObj) {
            return JSRuntime.booleanToString((boolean) thisObj);
        }
    }

    public abstract static class JSBooleanValueOfNode extends JSBuiltinNode {

        public JSBooleanValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"!isJSBoolean(thisObj)", "!isBoolean(thisObj)"})
        protected boolean valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw JSBoolean.noBooleanError();
        }

        @Specialization(guards = "isJSBoolean(thisObj)")
        protected boolean valueOf(DynamicObject thisObj) {
            return JSBoolean.valueOf(thisObj);
        }

        @Specialization(guards = "isBoolean(thisObj)")
        protected boolean valueOfPrimitive(Object thisObj) {
            return (boolean) thisObj;
        }
    }
}
