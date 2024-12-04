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
package com.oracle.truffle.js.builtins.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.json.JSONBuiltinsFactory.JSONIsRawJSONNodeGen;
import com.oracle.truffle.js.builtins.json.JSONBuiltinsFactory.JSONParseNodeGen;
import com.oracle.truffle.js.builtins.json.JSONBuiltinsFactory.JSONRawJSONNodeGen;
import com.oracle.truffle.js.builtins.json.JSONBuiltinsFactory.JSONStringifyNodeGen;
import com.oracle.truffle.js.builtins.json.TruffleJSONParser.Mode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSRawJSON;
import com.oracle.truffle.js.runtime.builtins.JSRawJSONObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSStringObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
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
        super(com.oracle.truffle.js.builtins.json.JSON.CLASS_NAME, JSON.class);
    }

    public enum JSON implements BuiltinEnum<JSON> {
        parse(2),
        stringify(3),

        rawJSON(1),
        isRawJSON(1);

        private final int length;

        JSON(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            return switch (this) {
                case rawJSON, isRawJSON -> JSConfig.StagingECMAScriptVersion;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, JSON builtinEnum) {
        switch (builtinEnum) {
            case rawJSON:
                return JSONRawJSONNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isRawJSON:
                return JSONIsRawJSONNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case parse:
                return JSONParseNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case stringify:
                return JSONStringifyNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSONRawJSONNode extends JSBuiltinNode {

        public JSONRawJSONNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object rawJSON(Object text,
                        @Cached JSToStringNode toStringNode) {
            TruffleString sourceText = toStringNode.executeString(text);
            JSRealm realm = getRealm();
            JSONParseNode.parseIntl(sourceText, Mode.RawJSON, realm);
            return JSRawJSON.create(getContext(), realm, sourceText);
        }
    }

    public abstract static class JSONIsRawJSONNode extends JSBuiltinNode {

        public JSONIsRawJSONNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static Object isRawJSON(Object value) {
            return value instanceof JSRawJSONObject;
        }
    }

    @ImportStatic(Strings.class)
    public abstract static class JSONParseNode extends JSBuiltinNode {

        public JSONParseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isUndefined(reviver)")
        protected Object parseString(TruffleString text, @SuppressWarnings("unused") Object reviver) {
            return parseIntl(text, Mode.WithoutReviver, getRealm());
        }

        @Specialization(guards = "!isCallable.executeBoolean(reviver)", replaces = "parseString")
        protected Object parseUnfiltered(Object text, @SuppressWarnings("unused") Object reviver,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached @Shared JSToStringNode toStringNode) {
            return parseIntl(toStringNode.executeString(text), Mode.WithoutReviver, getRealm());
        }

        @Specialization(guards = "isCallable.executeBoolean(reviver)")
        protected Object parse(Object value, Object reviver,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached @Shared JSToStringNode toStringNode,
                        @Cached("create(getContext(), EMPTY_STRING)") CreateDataPropertyNode createWrapperPropertyNode) {
            TruffleString text = toStringNode.executeString(value);
            boolean withSource = getContext().getEcmaScriptVersion() >= JSConfig.StagingECMAScriptVersion;
            JSRealm realm = getRealm();
            Object unfiltered = parseIntl(text, withSource ? Mode.WithReviverAndSource : Mode.WithReviver, realm);

            JSObject root = JSOrdinary.create(getContext(), realm);
            JSONParseRecord snapshot = null;
            if (withSource) {
                snapshot = (JSONParseRecord) unfiltered;
                unfiltered = snapshot.value();
            }
            createWrapperPropertyNode.executeVoid(root, unfiltered);
            return internalizeJSONProperty(root, Strings.EMPTY_STRING, reviver, text, snapshot, withSource);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static Object parseIntl(TruffleString jsonString, Mode mode, JSRealm realm) {
            return new TruffleJSONParser(realm.getContext(), mode).parse(jsonString, realm);
        }

        @TruffleBoundary
        private Object internalizeJSONProperty(JSObject holder, TruffleString property, Object reviverFn, TruffleString input, JSONParseRecord parseRecord, boolean withSource) {
            Object value = JSObject.get(holder, property);
            assert !(value instanceof JSONParseRecord);

            JSObject contextObject = JSOrdinary.create(getContext(), getRealm());

            boolean useParseRecord = false;
            if (parseRecord != null && JSRuntime.isSameValue(parseRecord.value(), value)) {
                useParseRecord = true;
                if (!JSRuntime.isObject(value)) {
                    JSObjectUtil.putDataProperty(contextObject, Strings.SOURCE, parseRecord.source(), JSAttributes.getDefault());
                }
            }
            if (JSRuntime.isObject(value)) {
                JSObject object = (JSObject) value;
                if (JSRuntime.isArray(object)) {
                    int len = (int) JSRuntime.toLength(JSObject.get(object, JSArray.LENGTH));
                    int elementRecordsLen = useParseRecord ? parseRecord.elements().size() : 0;
                    for (int i = 0; i < len; i++) {
                        TruffleString stringIndex = Strings.fromInt(i);
                        JSONParseRecord elementRecord = i < elementRecordsLen ? parseRecord.elements().get(i) : null;
                        Object newElement = internalizeJSONProperty(object, stringIndex, reviverFn, input, elementRecord, withSource);
                        if (newElement == Undefined.instance) {
                            JSObject.delete(object, i);
                        } else {
                            JSRuntime.createDataProperty(object, stringIndex, newElement);
                        }
                    }
                } else {
                    for (TruffleString p : JSObject.enumerableOwnNames(object)) {
                        JSONParseRecord entryRecord = useParseRecord ? parseRecord.entries().get(p) : null;
                        Object newElement = internalizeJSONProperty(object, p, reviverFn, input, entryRecord, withSource);
                        if (newElement == Undefined.instance) {
                            JSObject.delete(object, p);
                        } else {
                            JSRuntime.createDataProperty(object, p, newElement);
                        }
                    }
                }
            }

            return JSRuntime.call(reviverFn, holder, withSource
                            ? new Object[]{property, value, contextObject}
                            : new Object[]{property, value});
        }
    }

    @SuppressWarnings("truffle-inlining") // TruffleString nodes do not support object inlining.
    public abstract static class JSONStringifyNode extends JSBuiltinNode {

        @Child private JSONStringifyStringNode jsonStringifyStringNode;
        @Child private CreateDataPropertyNode createWrapperPropertyNode;

        public JSONStringifyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createWrapperPropertyNode = CreateDataPropertyNode.create(context, Strings.EMPTY_STRING);
            this.jsonStringifyStringNode = JSONStringifyStringNode.create(context);
        }

        @Specialization(guards = {"!isString(value)", "isUndefined(replacer)"})
        protected Object stringifyNoReplacer(Object value, @SuppressWarnings("unused") Object replacer, Object space,
                        @Cached @Exclusive GetGapNode getGapNode) {
            return stringifyIntl(value, space, null, null, this, getGapNode);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "!isUndefined(replacer)")
        protected Object stringifyWithReplacer(Object value, Object replacer, Object space,
                        @Bind Node node,
                        @Cached @Exclusive GetGapNode getGapNode,
                        @Cached IsCallableNode isCallableNode,
                        @Cached("createIsArrayLike()") JSIsArrayNode isArrayNode,
                        @Cached ToReplacerListNode toReplacerListNode) {
            Object replacerFn = null;
            List<Object> replacerList = null;
            if (isCallableNode.executeBoolean(replacer)) {
                replacerFn = replacer;
            } else if (isArrayNode.execute(replacer)) {
                replacerList = toReplacerListNode.execute(node, replacer);
            }
            return stringifyIntl(value, space, replacerFn, replacerList, node, getGapNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUndefined(replacer)"})
        // GR-24628: JSON.stringify is frequently called with (just) a String argument
        protected Object stringifyAStringNoReplacer(TruffleString str, Object replacer, Object space,
                        @Cached("createStringBuilderProfile()") StringBuilderProfile stringBuilderProfile,
                        @Cached TruffleString.ReadCharUTF16Node readCharNode,
                        @Cached TruffleStringBuilder.AppendCharUTF16Node appendRawValueNode,
                        @Cached TruffleStringBuilder.AppendStringNode appendStringNode,
                        @Cached TruffleStringBuilder.AppendSubstringByteIndexNode appendSubstringNode,
                        @Cached TruffleStringBuilder.ToStringNode builderToStringNode) {
            var builder = Strings.builderCreate(Strings.length(str) + 8);
            JSONStringifyStringNode.jsonQuote(stringBuilderProfile, builder, str, readCharNode, appendRawValueNode, appendStringNode, appendSubstringNode);
            return StringBuilderProfile.toString(builderToStringNode, builder);
        }

        protected StringBuilderProfile createStringBuilderProfile() {
            return StringBuilderProfile.create(getContext().getStringLengthLimit());
        }

        private Object stringifyIntl(Object value, Object space, Object replacerFnObj, List<Object> replacerList, Node node, GetGapNode getGapNode) {
            final TruffleString gap = getGapNode.execute(node, space);

            JSObject wrapper = JSOrdinary.create(getContext(), getRealm());
            createWrapperPropertyNode.executeVoid(wrapper, value);
            return jsonStringifyStringNode.execute(new JSONData(gap, replacerFnObj, replacerList), Strings.EMPTY_STRING, wrapper);
        }
    }

    @SuppressWarnings("truffle-inlining") // TruffleString nodes do not support object inlining.
    @GenerateInline
    @GenerateCached(false)
    abstract static class GetGapNode extends JavaScriptBaseNode {

        public abstract TruffleString execute(Node node, Object space);

        @Specialization(guards = "isUndefined(space)")
        static TruffleString doUndefined(@SuppressWarnings("unused") Object space) {
            return Strings.EMPTY_STRING;
        }

        @Specialization
        static TruffleString doNumberObject(JSNumberObject space,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached @Shared JSToIntegerAsIntNode toIntegerNode,
                        @Cached @Shared TruffleString.FromByteArrayNode fromByteArrayNode,
                        @Cached @Shared TruffleString.SwitchEncodingNode switchEncodingNode) {
            // Even though space is Number object, ToNumber may not return its [[NumberData]].
            return doNumber(toNumberNode.execute(space), toIntegerNode, fromByteArrayNode, switchEncodingNode);
        }

        @Specialization
        static TruffleString doStringObject(JSStringObject space,
                        @Cached JSToStringNode toStringNode,
                        @Cached @Shared TruffleString.SubstringByteIndexNode substringNode) {
            // Even though space is String object, ToString may not return its [[StringData]].
            return doString(toStringNode.executeString(space), substringNode);
        }

        @Specialization(guards = "isNumber(space)")
        static TruffleString doNumber(Object space,
                        @Cached @Shared JSToIntegerAsIntNode toIntegerNode,
                        @Cached @Shared TruffleString.FromByteArrayNode fromByteArrayNode,
                        @Cached @Shared TruffleString.SwitchEncodingNode switchEncodingNode) {
            int newSpace = Math.max(0, Math.min(10, toIntegerNode.executeInt(space)));
            byte[] ar = new byte[newSpace];
            Arrays.fill(ar, (byte) ' ');
            return switchEncodingNode.execute(fromByteArrayNode.execute(ar, TruffleString.Encoding.ISO_8859_1, false), TruffleString.Encoding.UTF_16);
        }

        @Specialization
        static TruffleString doLong(long space,
                        @Cached @Shared JSToIntegerAsIntNode toIntegerNode,
                        @Cached @Shared TruffleString.FromByteArrayNode fromByteArrayNode,
                        @Cached @Shared TruffleString.SwitchEncodingNode switchEncodingNode) {
            return doNumber(space, toIntegerNode, fromByteArrayNode, switchEncodingNode);
        }

        @Specialization
        static TruffleString doString(TruffleString space,
                        @Cached @Shared TruffleString.SubstringByteIndexNode substringNode) {
            if (Strings.length(space) <= 10) {
                return space;
            } else {
                return Strings.lazySubstring(substringNode, space, 0, 10);
            }
        }

        @Fallback
        static TruffleString doOther(@SuppressWarnings("unused") Object space) {
            return Strings.EMPTY_STRING;
        }
    }

    @GenerateInline
    @GenerateCached(false)
    abstract static class ToReplacerListNode extends JavaScriptBaseNode {

        public abstract List<Object> execute(Node node, Object replacer);

        @Specialization
        static List<Object> makeReplacerList(Object replacerObj,
                        @Cached(value = "create(getLanguage().getJSContext())", inline = false) JSGetLengthNode getLengthNode,
                        @Cached(value = "create(getLanguage().getJSContext())", inline = false) ReadElementNode getElementNode,
                        @Cached(inline = false) JSToStringNode toStringNode) {
            long len = getLengthNode.executeLong(replacerObj);
            List<Object> replacerList = new ArrayList<>();
            for (long k = 0; k < len; k++) {
                // harmony/proxies-json.js requires toString()
                Object v = getElementNode.executeWithTargetAndIndex(replacerObj, k);
                TruffleString item = null; // Let item be undefined.
                if (v instanceof TruffleString str) {
                    item = str;
                } else if (JSRuntime.isNumber(v) || v instanceof Long || JSNumber.isJSNumber(v) || JSString.isJSString(v)) {
                    item = toStringNode.executeString(v);
                }
                if (item != null) { // If item is not undefined ...
                    addToReplacer(replacerList, item);
                }
            }
            return replacerList;
        }

        @TruffleBoundary
        private static void addToReplacer(List<Object> replacerList, TruffleString item) {
            if (!replacerList.contains(item)) {
                replacerList.add(item);
            }
        }
    }
}
