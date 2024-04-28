/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.js.builtins.JSONBuiltinsFactory.JSONParseNodeGen;
import com.oracle.truffle.js.builtins.JSONBuiltinsFactory.JSONStringifyNodeGen;
import com.oracle.truffle.js.builtins.helper.JSONData;
import com.oracle.truffle.js.builtins.helper.JSONStringifyStringNode;
import com.oracle.truffle.js.builtins.helper.TruffleJSONParser;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;

/**
 * Contains builtins for {@linkplain JSON} function (constructor).
 */
public final class JSONBuiltins extends JSBuiltinsContainer.SwitchEnum<JSONBuiltins.JSON> {

    public static final JSBuiltinsContainer BUILTINS = new JSONBuiltins();

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

        protected TruffleString toString(Object target) {
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

        @Specialization(guards = "isCallable.executeBoolean(reviver)", limit = "1")
        protected Object parse(Object text, Object reviver,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            Object unfiltered = parseIntl(toString(text));
            JSObject root = JSOrdinary.create(getContext(), getRealm());
            JSObjectUtil.putDataProperty(root, Strings.EMPTY_STRING, unfiltered, JSAttributes.getDefault());
            return walk(reviver, root, Strings.EMPTY_STRING);
        }

        @Specialization(guards = "!isCallable.executeBoolean(reviver)", limit = "1")
        protected Object parseUnfiltered(Object text, @SuppressWarnings("unused") Object reviver,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            return parseIntl(toString(text));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private Object parseIntl(TruffleString jsonString) {
            return new TruffleJSONParser(getContext()).parse(jsonString, getRealm());
        }

        @TruffleBoundary
        private Object walk(Object reviverFn, JSObject holder, Object property) {
            Object value = JSObject.get(holder, property);
            if (JSRuntime.isObject(value)) {
                JSObject object = (JSObject) value;
                if (isArray(object)) {
                    int len = (int) JSRuntime.toLength(JSObject.get(object, JSArray.LENGTH));
                    for (int i = 0; i < len; i++) {
                        Object stringIndex = Strings.fromInt(i);
                        Object newElement = walk(reviverFn, object, stringIndex);
                        if (newElement == Undefined.instance) {
                            JSObject.delete(object, i);
                        } else {
                            JSRuntime.createDataProperty(object, stringIndex, newElement);
                        }
                    }
                } else {
                    for (Object p : JSObject.enumerableOwnNames(object)) {
                        Object newElement = walk(reviverFn, object, p);
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
        @Child private CreateDataPropertyNode createWrapperPropertyNode;
        @Child private JSToIntegerAsIntNode toIntegerNode;
        @Child private JSToNumberNode toNumberNode;
        @Child private JSIsArrayNode isArrayNode;
        @Child private IsCallableNode isCallableNode;
        private final BranchProfile spaceIsStringBranch = BranchProfile.create();
        private final ConditionProfile spaceIsUndefinedProfile = ConditionProfile.create();

        protected Object jsonStr(Object jsonData, Object keyStr, JSObject holder) {
            if (jsonStringifyStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                jsonStringifyStringNode = insert(JSONStringifyStringNode.create(getContext()));
            }
            return jsonStringifyStringNode.execute(jsonData, keyStr, holder);
        }

        @Override
        protected boolean isArray(Object replacer) {
            if (isArrayNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isArrayNode = insert(JSIsArrayNode.createIsArrayLike());
            }
            return isArrayNode.execute(replacer);
        }

        protected boolean isCallable(Object obj) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = insert(IsCallableNode.create());
            }
            return isCallableNode.executeBoolean(obj);
        }

        @Specialization(guards = "isCallable(replacerFn)")
        protected Object stringify(Object value, Object replacerFn, Object spaceParam) {
            assert JSRuntime.isCallable(replacerFn);
            return stringifyIntl(value, spaceParam, replacerFn, null);
        }

        @Specialization(guards = "isArray(replacerObj)")
        protected Object stringifyReplacerArray(Object value, JSDynamicObject replacerObj, Object spaceParam) {
            long len = JSRuntime.toLength(JSObject.get(replacerObj, JSArray.LENGTH));
            List<Object> replacerList = new ArrayList<>();
            for (long i = 0; i < len; i++) {
                // harmony/proxies-json.js requires toString()
                TruffleString k = Strings.fromLong(i);
                Object v = JSObject.get(replacerObj, k);
                Object item = null; // Let item be undefined.
                if (Strings.isTString(v)) {
                    item = JSRuntime.toStringIsString(v);
                } else if (JSRuntime.isNumber(v) || JSNumber.isJSNumber(v) || JSString.isJSString(v)) {
                    item = toString(v);
                }
                if (item != null) { // If item is not undefined ...
                    addToReplacer(replacerList, item);
                }
            }
            return stringifyIntl(value, spaceParam, null, replacerList);
        }

        @TruffleBoundary
        private static void addToReplacer(List<Object> replacerList, Object item) {
            if (!replacerList.contains(item)) {
                replacerList.add(item);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isCallable(replacer)", "!isArray(replacer)"})
        // GR-24628: JSON.stringify is frequently called with (just) a String argument
        protected Object stringifyAStringNoReplacer(TruffleString str, Object replacer, Object spaceParam,
                        @Cached("createStringBuilderProfile()") StringBuilderProfile stringBuilderProfile,
                        @Cached TruffleStringBuilder.AppendCharUTF16Node appendRawValueNode,
                        @Cached TruffleStringBuilder.AppendStringNode appendStringNode,
                        @Cached TruffleStringBuilder.ToStringNode builderToStringNode) {
            var builder = Strings.builderCreate(Strings.length(str) + 8);
            JSONStringifyStringNode.jsonQuote(stringBuilderProfile, builder, str, appendRawValueNode, appendStringNode);
            return StringBuilderProfile.toString(builderToStringNode, builder);
        }

        protected StringBuilderProfile createStringBuilderProfile() {
            return StringBuilderProfile.create(getContext().getStringLengthLimit());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isString(value)", "!isCallable(replacer)", "!isArray(replacer)"})
        protected Object stringifyNoReplacer(Object value, Object replacer, Object spaceParam) {
            return stringifyIntl(value, spaceParam, null, null);
        }

        private Object stringifyIntl(Object value, Object spaceParam, Object replacerFnObj, List<Object> replacerList) {
            final TruffleString gap = spaceIsUndefinedProfile.profile(spaceParam == Undefined.instance) ? Strings.EMPTY_STRING : getGap(spaceParam);

            JSObject wrapper = JSOrdinary.create(getContext(), getRealm());
            if (createWrapperPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createWrapperPropertyNode = insert(CreateDataPropertyNode.create(getContext(), Strings.EMPTY_STRING));
            }
            createWrapperPropertyNode.executeVoid(wrapper, value);
            return jsonStr(new JSONData(gap, replacerFnObj, replacerList), Strings.EMPTY_STRING, wrapper);
        }

        private TruffleString getGap(Object spaceParam) {
            Object space = spaceParam;
            if (JSDynamicObject.isJSDynamicObject(space)) {
                if (JSNumber.isJSNumber(space)) {
                    space = toNumber(space);
                } else if (JSString.isJSString(space)) {
                    space = toString(space);
                }
            }
            if (JSRuntime.isNumber(space) || space instanceof Long) {
                if (toIntegerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    toIntegerNode = insert(JSToIntegerAsIntNode.create());
                }
                int newSpace = Math.max(0, Math.min(10, toIntegerNode.executeInt(space)));
                return makeGap(newSpace);
            } else if (Strings.isTString(space)) {
                spaceIsStringBranch.enter();
                return makeGap(JSRuntime.toStringIsString(space));
            } else {
                return Strings.EMPTY_STRING;
            }
        }

        @TruffleBoundary
        private TruffleString makeGap(TruffleString spaceStr) {
            if (Strings.length(spaceStr) <= 10) {
                return spaceStr;
            } else {
                return Strings.substring(getContext(), spaceStr, 0, 10);
            }
        }

        @TruffleBoundary
        private static TruffleString makeGap(int spaceValue) {
            char[] ar = new char[spaceValue];
            Arrays.fill(ar, ' ');
            return Strings.fromCharArray(ar);
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
