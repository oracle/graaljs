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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.JSONBuiltinsFactory.JSONParseNodeGen;
import com.oracle.truffle.js.builtins.JSONBuiltinsFactory.JSONStringifyNodeGen;
import com.oracle.truffle.js.builtins.helper.JSONData;
import com.oracle.truffle.js.builtins.helper.JSONStringifyStringNode;
import com.oracle.truffle.js.builtins.helper.TruffleJSONParser;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSON} function (constructor).
 */
public final class JSONBuiltins extends JSBuiltinsContainer.SwitchEnum<JSONBuiltins.JSON> {
    protected JSONBuiltins() {
        super(com.oracle.truffle.js.runtime.builtins.JSON.CLASS_NAME, JSON.class);
    }

    public enum JSON implements BuiltinEnum<JSON> {
        parse(2),
        stringify(3);

        private final int length;

        JSON(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, JSON builtinEnum) {
        switch (builtinEnum) {
            case parse:
                return JSONParseNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case stringify:
                return JSONStringifyNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSONOperation extends JSBuiltinNode {
        public JSONOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToStringNode toStringNode;

        protected String toString(Object target) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(target);
        }

        protected boolean isArray(Object replacer) {
            return JSRuntime.isArray(replacer);
        }
    }

    public abstract static class JSONParseNode extends JSONOperation {

        public JSONParseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private TruffleJSONParser parser;

        @Specialization(guards = "isCallable(reviver)")
        protected Object parse(Object text, Object reviver) {
            Object unfiltered = parseIntl(toString(text));
            DynamicObject root = JSUserObject.create(getContext());
            JSObjectUtil.putDataProperty(getContext(), root, "", unfiltered, JSAttributes.getDefault());
            return walk((DynamicObject) reviver, root, "");
        }

        @Specialization(guards = "!isCallable(reviver)")
        protected Object parseUnfiltered(Object text, @SuppressWarnings("unused") Object reviver) {
            return parseIntl(toString(text));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private Object parseIntl(String jsonString) {
            if (JSTruffleOptions.TruffleJSONParser) {
                return parseJSON(jsonString);
            } else {
                return getContext().getEvaluator().parseJSON(getContext(), jsonString);
            }
        }

        private Object parseJSON(String jsonString) {
            if (parser == null) {
                parser = new TruffleJSONParser(getContext());
            }
            return parser.parse(jsonString);
        }

        @TruffleBoundary
        private Object walk(DynamicObject reviverFn, DynamicObject holder, String property) {
            Object value = JSObject.get(holder, property);
            if (JSRuntime.isObject(value)) {
                DynamicObject object = (DynamicObject) value;
                if (isArray(object)) {
                    int len = (int) JSRuntime.toLength(JSObject.get(object, JSArray.LENGTH));
                    for (int i = 0; i < len; i++) {
                        Object newElement = walk(reviverFn, object, Boundaries.stringValueOf(i));
                        if (newElement == Undefined.instance) {
                            JSObject.delete(object, i);
                        } else {
                            JSObject.set(object, i, newElement);
                        }
                    }
                } else {
                    for (String p : JSObject.enumerableOwnNames(object)) {
                        Object newElement = walk(reviverFn, object, Boundaries.stringValueOf(p));
                        if (newElement == Undefined.instance) {
                            JSObject.delete(object, p);
                        } else {
                            JSRuntime.createDataProperty(object, p, newElement);
                        }
                    }
                }
            }
            return JSRuntime.call(reviverFn, holder, new Object[]{property, value});
        }
    }

    public abstract static class JSONStringifyNode extends JSONOperation {

        public JSONStringifyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSONStringifyStringNode jsonStringifyStringNode;
        @Child private PropertySetNode setWrapperProperty;
        @Child private JSToIntegerNode toIntegerNode;
        @Child private JSToNumberNode toNumberNode;
        private final BranchProfile spaceIsStringBranch = BranchProfile.create();

        protected Object jsonStr(Object jsonData, String key, DynamicObject holder) {
            if (jsonStringifyStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                jsonStringifyStringNode = insert(JSONStringifyStringNode.create(getContext()));
            }
            return jsonStringifyStringNode.execute(jsonData, key, holder);
        }

        @Specialization(guards = "isCallable(replacerFn)")
        protected Object stringify(Object value, DynamicObject replacerFn, Object spaceParam) {
            assert JSRuntime.isCallable(replacerFn);
            return stringifyIntl(value, spaceParam, replacerFn, null);
        }

        @Specialization(guards = "isArray(replacerObj)")
        protected Object stringifyReplacerArray(Object value, DynamicObject replacerObj, Object spaceParam) {
            int len = (int) JSRuntime.toLength(JSObject.get(replacerObj, JSArray.LENGTH));
            List<String> replacerList = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                // harmony/proxies-json.js requires toString()
                Object v = JSObject.get(replacerObj, JSRuntime.toString(i));
                String item = "";
                if (JSRuntime.isString(v)) {
                    item = Boundaries.javaToString(v);
                } else if (JSRuntime.isNumber(v) || JSNumber.isJSNumber(v) || JSString.isJSString(v)) {
                    item = toString(v);
                }
                addToReplacer(replacerList, item);
            }
            return stringifyIntl(value, spaceParam, null, replacerList);
        }

        @TruffleBoundary
        private static void addToReplacer(List<String> replacerList, String item) {
            if (!replacerList.contains(item)) {
                replacerList.add(item);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isCallable(replacer)", "!isArray(replacer)"})
        protected Object stringifyNoReplacer(Object value, Object replacer, Object spaceParam) {
            return stringifyIntl(value, spaceParam, null, null);
        }

        private Object stringifyIntl(Object value, Object spaceParam, DynamicObject replacerFnObj, List<String> replacerList) {
            Object space = spaceParam;
            if (JSObject.isDynamicObject(space)) {
                if (JSNumber.isJSNumber(spaceParam)) {
                    space = toNumber(space);
                } else if (JSString.isJSString(space)) {
                    space = toString(space);
                }
            }
            final String gap;
            if (JSRuntime.isNumber(space)) {
                if (toIntegerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    toIntegerNode = insert(JSToIntegerNode.create());
                }
                int newSpace = Math.max(0, Math.min(10, toIntegerNode.executeInt(space)));
                gap = makeGap(newSpace);
            } else if (JSRuntime.isString(space)) {
                spaceIsStringBranch.enter();
                gap = makeGap(Boundaries.javaToString(space));
            } else {
                gap = "";
            }

            DynamicObject wrapper = JSUserObject.create(getContext());
            if (setWrapperProperty == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setWrapperProperty = insert(PropertySetNode.create("", false, getContext(), false));
            }
            setWrapperProperty.setValue(wrapper, value);
            return jsonStr(new JSONData(gap, replacerFnObj, replacerList), "", wrapper);
        }

        @TruffleBoundary
        private static String makeGap(String spaceStr) {
            if (spaceStr.length() <= 10) {
                return spaceStr;
            } else {
                return spaceStr.substring(0, 10);
            }
        }

        @TruffleBoundary
        private static String makeGap(int spaceValue) {
            char[] ar = new char[spaceValue];
            Arrays.fill(ar, ' ');
            return new String(ar);
        }

        protected Number toNumber(Object target) {
            if (toNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumberNode = insert(JSToNumberNode.create());
            }
            return toNumberNode.executeNumber(target);
        }
    }
}
