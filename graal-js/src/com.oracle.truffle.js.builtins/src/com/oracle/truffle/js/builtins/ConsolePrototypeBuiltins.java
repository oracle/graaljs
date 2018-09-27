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
import com.oracle.truffle.js.builtins.ConsolePrototypeBuiltinsFactory.JSConsoleAssertNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltins.JSGlobalPrintNode;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalPrintNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for `console`.
 */
public final class ConsolePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ConsolePrototypeBuiltins.ConsolePrototype> {
    protected ConsolePrototypeBuiltins() {
        super("Console", ConsolePrototype.class);
    }

    public enum ConsolePrototype implements BuiltinEnum<ConsolePrototype> {
        log(1),
        info(1),
        debug(1),
        error(1),
        warn(1),
        assert_(2);

        private final int length;

        ConsolePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ConsolePrototype builtinEnum) {
        switch (builtinEnum) {
            case log:
            case info:
            case debug:
                return JSGlobalPrintNodeGen.create(context, builtin, false, args().varArgs().createArgumentNodes(context));
            case error:
            case warn:
                return JSGlobalPrintNodeGen.create(context, builtin, true, args().varArgs().createArgumentNodes(context));
            case assert_:
                return JSConsoleAssertNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSConsoleOperation extends JSBuiltinNode {

        public JSConsoleOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }
    }

    public abstract static class JSConsoleAssertNode extends JSConsoleOperation {

        @Child private JSGlobalPrintNode printNode;
        @Child private JSToBooleanNode toBooleanNode;

        public JSConsoleAssertNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            printNode = JSGlobalPrintNodeGen.create(context, null, false, null);
            toBooleanNode = JSToBooleanNode.create();
        }

        @Specialization
        protected DynamicObject _assert(Object... data) {
            boolean result = data.length > 0 ? toBooleanNode.executeBoolean(data[0]) : false;
            if (!result) {
                Object arr[] = new Object[data.length > 0 ? data.length : 1];
                if (data.length > 1) {
                    System.arraycopy(data, 1, arr, 1, data.length - 1);
                }
                arr[0] = "Assertion failed:";
                printNode.executeObjectArray(arr);
            }
            return Undefined.instance;
        }
    }
}
