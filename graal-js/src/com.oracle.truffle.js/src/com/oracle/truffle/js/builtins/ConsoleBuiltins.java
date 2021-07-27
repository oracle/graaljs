/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleAssertNodeGen;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleClearNodeGen;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleCountNodeGen;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleCountResetNodeGen;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleGroupEndNodeGen;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleGroupNodeGen;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleTimeEndNodeGen;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleTimeLogNodeGen;
import com.oracle.truffle.js.builtins.ConsoleBuiltinsFactory.JSConsoleTimeNodeGen;
import com.oracle.truffle.js.builtins.GlobalBuiltins.JSGlobalPrintNode;
import com.oracle.truffle.js.builtins.GlobalBuiltinsFactory.JSGlobalPrintNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSConsoleUtil;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for `console`.
 */
public final class ConsoleBuiltins extends JSBuiltinsContainer.SwitchEnum<ConsoleBuiltins.Console> {

    public static final JSBuiltinsContainer BUILTINS = new ConsoleBuiltins();

    protected ConsoleBuiltins() {
        super(JSRealm.CONSOLE_CLASS_NAME, Console.class);
    }

    public enum Console implements BuiltinEnum<Console> {
        log(0),
        info(0),
        debug(0),
        dir(0),
        error(0),
        warn(0),
        assert_(0),
        clear(0),
        count(0),
        countReset(0),
        group(0),
        groupCollapsed(0),
        groupEnd(0),
        time(0),
        timeEnd(0),
        timeLog(0);

        private final int length;

        Console(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Console builtinEnum) {
        switch (builtinEnum) {
            case log:
            case info:
            case debug:
            case dir: // dir is not a strict alias of log, but close enough for our purpose
                return JSGlobalPrintNodeGen.create(context, builtin, false, args().varArgs().createArgumentNodes(context));
            case error:
            case warn:
                return JSGlobalPrintNodeGen.create(context, builtin, true, args().varArgs().createArgumentNodes(context));
            case assert_:
                return JSConsoleAssertNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case clear:
                return JSConsoleClearNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case count:
                return JSConsoleCountNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case countReset:
                return JSConsoleCountResetNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case group:
            case groupCollapsed:
                return JSConsoleGroupNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case groupEnd:
                return JSConsoleGroupEndNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
            case time:
                return JSConsoleTimeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case timeEnd:
                return JSConsoleTimeEndNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case timeLog:
                return JSConsoleTimeLogNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSConsoleOperation extends JSBuiltinNode {

        protected static final String DEFAULT = "default";

        public JSConsoleOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public JSConsoleUtil getConsoleUtil() {
            return getRealm().getConsoleUtil();
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
        protected DynamicObject assertImpl(Object... data) {
            boolean result = data.length > 0 ? toBooleanNode.executeBoolean(data[0]) : false;
            if (!result) {
                Object[] arr = new Object[data.length > 0 ? data.length : 1];
                if (data.length > 1) {
                    System.arraycopy(data, 1, arr, 1, data.length - 1);
                }
                arr[0] = data.length > 1 ? "Assertion failed:" : "Assertion failed";
                printNode.executeObjectArray(arr);
            }
            return Undefined.instance;
        }
    }

    public abstract static class JSConsoleClearNode extends JSConsoleOperation {

        public JSConsoleClearNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected DynamicObject clear() {
            PrintWriter writer = getRealm().getOutputWriter();
            writer.append("\033[H\033[2J");
            writer.flush();
            return Undefined.instance;
        }
    }

    public abstract static class JSConsoleCountNode extends JSConsoleOperation {

        @Child private JSToStringNode toStringNode;

        public JSConsoleCountNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toStringNode = JSToStringNode.create();
        }

        @Specialization
        @TruffleBoundary
        protected DynamicObject count(Object label) {
            String key = label == Undefined.instance ? DEFAULT : toStringNode.executeString(label);
            int count = 0;
            JSConsoleUtil console = getConsoleUtil();
            Map<String, Integer> countMap = console.getCountMap();
            if (countMap.containsKey(key)) {
                count = countMap.get(key);
            }
            countMap.put(key, ++count);

            PrintWriter writer = getRealm().getOutputWriter();
            writer.append(console.getConsoleIndentationString());
            writer.append(key);
            writer.append(": ");
            writer.append(String.valueOf(count));
            writer.append(JSRuntime.LINE_SEPARATOR);
            writer.flush();
            return Undefined.instance;
        }
    }

    public abstract static class JSConsoleCountResetNode extends JSConsoleOperation {

        @Child private JSToStringNode toStringNode;

        public JSConsoleCountResetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toStringNode = JSToStringNode.create();
        }

        @Specialization
        @TruffleBoundary
        protected DynamicObject count(Object label) {
            String key = label == Undefined.instance ? DEFAULT : toStringNode.executeString(label);
            getConsoleUtil().getCountMap().remove(key);
            return Undefined.instance;
        }
    }

    public abstract static class JSConsoleGroupNode extends JSConsoleOperation {

        @Child private JSGlobalPrintNode printNode;
        @Child private JSToStringNode toStringNode;

        public JSConsoleGroupNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toStringNode = JSToStringNode.create();
            printNode = JSGlobalPrintNodeGen.create(context, null, false, null);
        }

        @Specialization
        @TruffleBoundary
        protected DynamicObject group(Object[] label) {
            if (label.length > 0) {
                printNode.executeObjectArray(label);
            }
            getConsoleUtil().incConsoleIndentation();
            return Undefined.instance;
        }
    }

    public abstract static class JSConsoleGroupEndNode extends JSConsoleOperation {

        public JSConsoleGroupEndNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

        }

        @Specialization
        @TruffleBoundary
        protected DynamicObject groupEnd() {
            getConsoleUtil().decConsoleIndentation();
            return Undefined.instance;
        }
    }

    public abstract static class JSConsoleTimeNode extends JSConsoleOperation {
        @Child private JSToStringNode toStringNode;

        public JSConsoleTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toStringNode = JSToStringNode.create();
        }

        @Specialization
        @TruffleBoundary
        protected DynamicObject time(Object label) {
            String key = label == Undefined.instance ? DEFAULT : toStringNode.executeString(label);
            getConsoleUtil().getTimeMap().put(key, getRealm().currentTimeMillis());
            return Undefined.instance;
        }
    }

    public abstract static class JSConsoleTimeEndNode extends JSConsoleOperation {
        @Child private JSToStringNode toStringNode;
        @Child private JSGlobalPrintNode printNode;

        public JSConsoleTimeEndNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toStringNode = JSToStringNode.create();
            printNode = JSGlobalPrintNodeGen.create(context, null, false, null);
        }

        @Specialization
        @TruffleBoundary
        protected DynamicObject timeEnd(Object label) {
            String key = label == Undefined.instance ? DEFAULT : toStringNode.executeString(label);
            Map<String, Long> timeMap = getConsoleUtil().getTimeMap();
            if (timeMap.containsKey(key)) {
                long start = timeMap.remove(key);
                long end = getRealm().currentTimeMillis();
                long delta = end - start;
                printNode.executeObjectArray(new Object[]{key + ":", String.valueOf(delta) + "ms"});
            }
            return Undefined.instance;
        }
    }

    public abstract static class JSConsoleTimeLogNode extends JSConsoleOperation {

        @Child private JSGlobalPrintNode printNode;
        @Child private JSToStringNode toStringNode;

        public JSConsoleTimeLogNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            printNode = JSGlobalPrintNodeGen.create(context, null, false, null);
            toStringNode = JSToStringNode.create();
        }

        @Specialization
        @TruffleBoundary
        protected DynamicObject timeLog(Object... data) {
            String key = data.length == 0 || data[0] == Undefined.instance ? DEFAULT : toStringNode.executeString(data[0]);
            Map<String, Long> timeMap = getConsoleUtil().getTimeMap();
            if (timeMap.containsKey(key)) {
                long start = timeMap.get(key);
                long end = getRealm().currentTimeMillis();
                long delta = end - start;

                Object[] arr = new Object[Math.max(2, data.length + 1)]; // add two, ignore first
                if (data.length > 1) {
                    System.arraycopy(data, 1, arr, 2, data.length - 1);
                }
                arr[0] = key + ":";
                arr[1] = String.valueOf(delta) + "ms";
                printNode.executeObjectArray(arr);
            }
            return Undefined.instance;
        }
    }
}
