/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.helper;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;

public abstract class JSONStringifyStringNode extends JavaScriptBaseNode {

    private final JSContext context;
    @Child private PropertyGetNode getToJSONProperty;
    @Child private JSFunctionCallNode callToJSONFunction;

    @Child private TruffleStringBuilder.AppendCharUTF16Node appendCharNode;
    @Child private TruffleStringBuilder.AppendIntNumberNode appendIntNode;
    @Child private TruffleStringBuilder.AppendLongNumberNode appendLongNode;
    @Child private TruffleStringBuilder.AppendStringNode appendStringNode;
    @Child private TruffleStringBuilder.ToStringNode builderToStringNode;

    private final StringBuilderProfile stringBuilderProfile;

    protected JSONStringifyStringNode(JSContext context) {
        this.context = context;
        this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
    }

    public abstract Object execute(Object data, Object keyStr, JSDynamicObject holder);

    public static JSONStringifyStringNode create(JSContext context) {
        return JSONStringifyStringNodeGen.create(context);
    }

    @Specialization
    public Object jsonStrMain(Object jsonData, TruffleString keyStr, JSDynamicObject holder) {
        try {
            assert jsonData instanceof JSONData;
            JSONData data = (JSONData) jsonData;
            Object value = jsonStrPrepare(data, keyStr, holder);
            if (!isStringifyable(value)) {
                return Undefined.instance;
            }
            TruffleStringBuilder sb = stringBuilderProfile.newStringBuilder();
            jsonStrExecute(sb, data, value);
            return builderToString(sb);
        } catch (StackOverflowError ex) {
            throwStackError();
            return null;
        }
    }

    private static boolean isStringifyable(Object value) {
        // values that are not stringifyable are replaced by undefined in jsonStrPrepare()
        return value != Undefined.instance;
    }

    @TruffleBoundary
    private void jsonStrExecute(TruffleStringBuilder builder, JSONData data, Object value) {
        assert isStringifyable(value);
        if (value == Null.instance) {
            append(builder, Null.NAME);
        } else if (value instanceof Boolean) {
            append(builder, (boolean) value ? JSBoolean.TRUE_NAME : JSBoolean.FALSE_NAME);
        } else if (Strings.isTString(value)) {
            jsonQuote(builder, (TruffleString) value);
        } else if (JSRuntime.isNumber(value)) {
            appendNumber(builder, (Number) value);
        } else if (JSRuntime.isBigInt(value)) {
            throw Errors.createTypeError("Do not know how to serialize a BigInt");
        } else if (JSDynamicObject.isJSDynamicObject(value) && !JSRuntime.isCallableIsJSObject((JSDynamicObject) value)) {
            JSDynamicObject valueObj = (JSDynamicObject) value;
            if (JSRuntime.isArray(valueObj)) {
                jsonJA(builder, data, valueObj);
            } else {
                jsonJO(builder, data, valueObj);
            }
        } else if (value instanceof TruffleObject) {
            assert JSGuards.isForeignObject(value);
            jsonForeignObject(builder, data, value);
        } else if (JSRuntime.isJavaPrimitive(value)) {
            // call toString on Java objects, GR-3722
            jsonQuote(builder, Strings.fromJavaString(value.toString()));
        } else {
            throw new RuntimeException("JSON.stringify: should never reach here, unknown type: " + value + " " + value.getClass());
        }
    }

    private void jsonForeignObject(TruffleStringBuilder sb, JSONData data, Object obj) {
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(obj);
        if (interop.isNull(obj)) {
            append(sb, Null.NAME);
        } else if (JSInteropUtil.isBoxedPrimitive(obj, interop)) {
            Object unboxed = JSInteropUtil.toPrimitiveOrDefault(obj, Null.instance, interop, this);
            assert !JSGuards.isForeignObject(unboxed);
            jsonStrExecute(sb, data, unboxed);
        } else if (interop.hasArrayElements(obj)) {
            jsonJA(sb, data, obj);
        } else {
            jsonJO(sb, data, obj);
        }
    }

    private void appendNumber(TruffleStringBuilder builder, Number n) {
        double d = JSRuntime.doubleValue(n);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            append(builder, Null.NAME);
        } else if (n instanceof Integer) {
            append(builder, ((Integer) n).intValue());
        } else if (n instanceof Long) {
            append(builder, ((Long) n).longValue());
        } else {
            append(builder, JSRuntime.doubleToString(d));
        }
    }

    @TruffleBoundary
    private Object jsonStrPrepare(JSONData data, TruffleString keyStr, Object holder) {
        Object value;
        if (JSDynamicObject.isJSDynamicObject(holder)) {
            value = JSObject.get((JSDynamicObject) holder, keyStr);
        } else {
            value = truffleRead(holder, keyStr);
        }
        return jsonStrPreparePart2(data, keyStr, holder, value);
    }

    @TruffleBoundary
    private Object jsonStrPrepareArray(JSONData data, int key, JSDynamicObject holder) {
        Object value = JSObject.get(holder, key);
        return jsonStrPreparePart2(data, Strings.fromInt(key), holder, value);
    }

    @TruffleBoundary
    private Object jsonStrPrepareForeign(JSONData data, int key, Object holder) {
        assert JSGuards.isForeignObject(holder);
        Object value = truffleRead(holder, key);
        return jsonStrPreparePart2(data, Strings.fromInt(key), holder, value);
    }

    private Object jsonStrPreparePart2(JSONData data, Object key, Object holder, Object valueArg) {
        Object value = valueArg;
        boolean tryToJSON = false;
        if (JSRuntime.isObject(value) || JSRuntime.isBigInt(value)) {
            tryToJSON = true;
        } else if (JSRuntime.isForeignObject(value)) {
            InteropLibrary interop = InteropLibrary.getUncached(value);
            tryToJSON = interop.hasMembers(value) && !interop.isNull(value) && !JSInteropUtil.isBoxedPrimitive(value, interop);
        }
        if (tryToJSON) {
            value = jsonStrPrepareObject(key, value);
        }

        if (data.getReplacerFnObj() != null) {
            value = JSRuntime.call(data.getReplacerFnObj(), holder, new Object[]{key, value});
        }
        if (JSDynamicObject.isJSDynamicObject(value)) {
            return jsonStrPrepareJSObject((JSDynamicObject) value);
        } else if (value instanceof Symbol) {
            return Undefined.instance;
        } else if (JSRuntime.isCallableForeign(value)) {
            return Undefined.instance;
        }
        return value;
    }

    private static Object jsonStrPrepareJSObject(JSDynamicObject valueObj) {
        JSClass builtinClass = JSObject.getJSClass(valueObj);
        if (builtinClass == JSNumber.INSTANCE) {
            return JSRuntime.toNumber(valueObj);
        } else if (builtinClass == JSBigInt.INSTANCE) {
            return JSBigInt.valueOf(valueObj);
        } else if (builtinClass == JSString.INSTANCE) {
            return JSRuntime.toString(valueObj);
        } else if (builtinClass == JSBoolean.INSTANCE) {
            return JSBoolean.valueOf(valueObj);
        } else if (JSRuntime.isCallableIsJSObject(valueObj)) {
            return Undefined.instance;
        }
        return valueObj;
    }

    private Object jsonStrPrepareObject(Object key, Object value) {
        assert JSRuntime.isPropertyKey(key);
        if (getToJSONProperty == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getToJSONProperty = insert(PropertyGetNode.create(Strings.TO_JSON, false, context));
        }
        Object toJSON = getToJSONProperty.getValue(value);
        if (JSRuntime.isCallable(toJSON)) {
            return jsonStrPrepareObjectFunction(key, value, toJSON);
        }
        return value;
    }

    private Object jsonStrPrepareObjectFunction(Object key, Object value, Object toJSON) {
        if (callToJSONFunction == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callToJSONFunction = insert(JSFunctionCallNode.createCall());
        }
        return callToJSONFunction.executeCall(JSArguments.createOneArg(value, toJSON, key));
    }

    @TruffleBoundary
    private TruffleStringBuilder jsonJO(TruffleStringBuilder sb, JSONData data, Object value) {
        checkCycle(data, value);
        data.pushStack(value);
        checkStackDepth(data);
        int stepback = data.getIndent();
        int indent = data.getIndent() + 1;
        data.setIndent(indent);

        concatStart(sb, '{');
        int lengthBefore = StringBuilderProfile.length(sb);
        if (data.getPropertyList() == null) {
            if (JSDynamicObject.isJSDynamicObject(value)) {
                serializeJSONObjectProperties(sb, data, value, indent, JSObject.enumerableOwnNames((JSDynamicObject) value));
            } else {
                serializeForeignObjectProperties(sb, data, value, indent);
            }
        } else {
            serializeJSONObjectProperties(sb, data, value, indent, data.getPropertyList());
        }
        concatEnd(sb, data, stepback, '}', lengthBefore != StringBuilderProfile.length(sb));

        data.popStack();
        data.setIndent(stepback);
        return sb;
    }

    private TruffleStringBuilder serializeJSONObjectProperties(TruffleStringBuilder sb, JSONData data, Object value, int indent, List<? extends Object> keys) {
        boolean isFirst = true;
        for (Object key : keys) {
            TruffleString name = (TruffleString) key;
            Object strPPrepared = jsonStrPrepare(data, name, value);
            if (isStringifyable(strPPrepared)) {
                if (isFirst) {
                    concatFirstStep(sb, data);
                    isFirst = false;
                } else {
                    appendSeparator(sb, data, indent);
                }
                jsonQuote(sb, name);
                appendColon(sb, data);
                jsonStrExecute(sb, data, strPPrepared);
            }
        }
        return sb;
    }

    private void appendColon(TruffleStringBuilder sb, JSONData data) {
        append(sb, ':');
        if (Strings.length(data.getGap()) > 0) {
            append(sb, ' ');
        }
    }

    private void serializeForeignObjectProperties(TruffleStringBuilder sb, JSONData data, Object obj, int indent) {
        try {
            InteropLibrary objInterop = InteropLibrary.getFactory().getUncached(obj);
            if (!objInterop.hasMembers(obj)) {
                return;
            }
            Object keysObj = objInterop.getMembers(obj);
            InteropLibrary keysInterop = InteropLibrary.getFactory().getUncached(keysObj);
            long size = keysInterop.getArraySize(keysObj);
            boolean isFirst = true;
            for (long i = 0; i < size; i++) {
                Object key = keysInterop.readArrayElement(keysObj, i);
                assert InteropLibrary.getUncached().isString(key);
                TruffleString stringKey = key instanceof TruffleString ? (TruffleString) key : InteropLibrary.getUncached().asTruffleString(key);
                if (!objInterop.isMemberReadable(obj, Strings.toJavaString(stringKey))) {
                    continue;
                }
                Object memberValue = truffleRead(obj, stringKey);
                Object strPPrepared = jsonStrPreparePart2(data, stringKey, obj, memberValue);
                if (isStringifyable(strPPrepared)) {
                    if (isFirst) {
                        concatFirstStep(sb, data);
                        isFirst = false;
                    } else {
                        appendSeparator(sb, data, indent);
                    }
                    jsonQuote(sb, stringKey);
                    appendColon(sb, data);
                    jsonStrExecute(sb, data, strPPrepared);
                }
            }
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "SerializeJSONObject", this);
        }
    }

    @TruffleBoundary
    private TruffleStringBuilder jsonJA(TruffleStringBuilder sb, JSONData data, Object value) {
        checkCycle(data, value);
        assert JSRuntime.isArray(value) || InteropLibrary.getUncached().hasArrayElements(value);
        data.pushStack(value);
        checkStackDepth(data);
        int stepback = data.getIndent();
        int indent = data.getIndent() + 1;
        data.setIndent(indent);
        Object lenObject;
        boolean isForeign = false;
        boolean isArray = false;
        if (JSDynamicObject.isJSDynamicObject(value)) { // Array or Proxy
            lenObject = JSObject.get((JSDynamicObject) value, JSArray.LENGTH);
            if (JSArray.isJSArray(value)) {
                isArray = true;
            }
        } else {
            lenObject = truffleGetSize(value);
            isForeign = true;
        }
        // output will reach maximum length in at most in StringLengthLimit steps
        long length = JSRuntime.toLength(lenObject);
        if (length > context.getStringLengthLimit()) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        int len = (int) length;
        concatStart(sb, '[');
        for (int index = 0; index < len; index++) {
            if (index == 0) {
                concatFirstStep(sb, data);
            } else {
                appendSeparator(sb, data, indent);
            }
            Object strPPrepared;
            if (isArray) {
                strPPrepared = jsonStrPrepareArray(data, index, (JSDynamicObject) value);
            } else if (isForeign) {
                strPPrepared = jsonStrPrepareForeign(data, index, value);
            } else {
                strPPrepared = jsonStrPrepare(data, Strings.fromInt(index), value);
            }
            if (isStringifyable(strPPrepared)) {
                jsonStrExecute(sb, data, strPPrepared);
            } else {
                append(sb, Null.NAME);
            }
        }

        concatEnd(sb, data, stepback, ']', len > 0);

        data.popStack();
        data.setIndent(stepback);
        return sb;
    }

    private static void checkStackDepth(JSONData data) {
        if (data.stackTooDeep()) {
            throwStackError();
        }
    }

    private static void throwStackError() {
        throw Errors.createRangeError("cannot stringify objects nested that deep");
    }

    private void concatStart(TruffleStringBuilder builder, char c) {
        append(builder, c);
    }

    private void concatFirstStep(TruffleStringBuilder sb, JSONData data) {
        if (Strings.length(data.getGap()) > 0) {
            append(sb, '\n');
            for (int i = 0; i < data.getIndent(); i++) {
                append(sb, data.getGap());
            }
        }
    }

    private void concatEnd(TruffleStringBuilder sb, JSONData data, int stepback, char close, boolean hasContent) {
        if (Strings.length(data.getGap()) > 0 && hasContent) {
            append(sb, '\n');
            for (int i = 0; i < stepback; i++) {
                append(sb, data.getGap());
            }
        }
        append(sb, close);
    }

    @TruffleBoundary
    private void appendSeparator(TruffleStringBuilder sb, JSONData data, int indent) {
        if (Strings.length(data.getGap()) <= 0) {
            append(sb, ',');
        } else {
            append(sb, Strings.COMMA_NEWLINE);
            for (int i = 0; i < indent; i++) {
                append(sb, data.getGap());
            }
        }
    }

    private static void checkCycle(JSONData data, Object value) {
        if (data.stack.contains(value)) {
            throw Errors.createTypeError("Converting circular structure to JSON");
        }
    }

    private TruffleStringBuilder jsonQuote(TruffleStringBuilder builder, TruffleString valueStr) {
        return jsonQuote(stringBuilderProfile, builder, valueStr, getAppendCharNode(), getAppendStringNode());
    }

    public static TruffleStringBuilder jsonQuote(StringBuilderProfile stringBuilderProfile, TruffleStringBuilder sb, TruffleString valueStr,
                    TruffleStringBuilder.AppendCharUTF16Node appendCharNode,
                    TruffleStringBuilder.AppendStringNode appendStringNode) {
        stringBuilderProfile.append(appendCharNode, sb, '"');
        for (int i = 0; i < Strings.length(valueStr);) {
            char ch = Strings.charAt(valueStr, i);
            if (ch < ' ') {
                if (ch == '\b') {
                    stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_B);
                } else if (ch == '\f') {
                    stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_F);
                } else if (ch == '\n') {
                    stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_N);
                } else if (ch == '\r') {
                    stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_R);
                } else if (ch == '\t') {
                    stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_T);
                } else {
                    jsonQuoteUnicode(stringBuilderProfile, sb, ch, appendCharNode, appendStringNode);
                }
            } else {
                if (ch == '\\') {
                    stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_BACKSLASH);
                } else if (ch == '"') {
                    stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_DOUBLE_QUOTE);
                } else if (Character.isSurrogate(ch)) {
                    if (Character.isHighSurrogate(ch)) {
                        char nextCh;
                        if (i + 1 < Strings.length(valueStr) && (Character.isLowSurrogate(nextCh = Strings.charAt(valueStr, i + 1)))) {
                            // paired surrogates
                            stringBuilderProfile.append(appendCharNode, sb, ch);
                            stringBuilderProfile.append(appendCharNode, sb, nextCh);
                            i++;
                        } else {
                            // unpaired high surrogate
                            jsonQuoteSurrogate(stringBuilderProfile, sb, ch, appendCharNode, appendStringNode);
                        }
                    } else {
                        // unpaired low surrogate
                        jsonQuoteSurrogate(stringBuilderProfile, sb, ch, appendCharNode, appendStringNode);
                    }
                } else {
                    stringBuilderProfile.append(appendCharNode, sb, ch);
                }
            }
            i++;
        }
        stringBuilderProfile.append(appendCharNode, sb, '"');
        return sb;
    }

    private static void jsonQuoteUnicode(StringBuilderProfile stringBuilderProfile, TruffleStringBuilder sb, char c,
                    TruffleStringBuilder.AppendCharUTF16Node appendCharNode,
                    TruffleStringBuilder.AppendStringNode appendStringNode) {
        stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_U00);
        stringBuilderProfile.append(appendCharNode, sb, Character.forDigit((c >> 4) & 0xF, 16));
        stringBuilderProfile.append(appendCharNode, sb, Character.forDigit(c & 0xF, 16));
    }

    private static void jsonQuoteSurrogate(StringBuilderProfile stringBuilderProfile, TruffleStringBuilder sb, char c,
                    TruffleStringBuilder.AppendCharUTF16Node appendCharNode,
                    TruffleStringBuilder.AppendStringNode appendStringNode) {
        stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_UD);
        stringBuilderProfile.append(appendCharNode, sb, Character.forDigit((c >> 8) & 0xF, 16));
        stringBuilderProfile.append(appendCharNode, sb, Character.forDigit((c >> 4) & 0xF, 16));
        stringBuilderProfile.append(appendCharNode, sb, Character.forDigit(c & 0xF, 16));
    }

    private Object truffleGetSize(Object obj) {
        return JSInteropUtil.getArraySize(obj, InteropLibrary.getUncached(), this);
    }

    private Object truffleRead(Object obj, TruffleString keyStr) {
        try {
            return JSRuntime.importValue(InteropLibrary.getUncached().readMember(obj, Strings.toJavaString(keyStr)));
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "readMember", keyStr, this);
        }
    }

    private Object truffleRead(Object obj, int index) {
        try {
            return JSRuntime.importValue(InteropLibrary.getUncached().readArrayElement(obj, index));
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "readArrayElement", index, this);
        }
    }

    private TruffleStringBuilder.AppendStringNode getAppendStringNode() {
        if (appendStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            appendStringNode = insert(TruffleStringBuilder.AppendStringNode.create());
        }
        return appendStringNode;
    }

    private void append(TruffleStringBuilder sb, TruffleString s) {
        stringBuilderProfile.append(getAppendStringNode(), sb, s);
    }

    private TruffleStringBuilder.AppendCharUTF16Node getAppendCharNode() {
        if (appendCharNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            appendCharNode = insert(TruffleStringBuilder.AppendCharUTF16Node.create());
        }
        return appendCharNode;
    }

    private void append(TruffleStringBuilder sb, char value) {
        stringBuilderProfile.append(getAppendCharNode(), sb, value);
    }

    private void append(TruffleStringBuilder sb, int value) {
        if (appendIntNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            appendIntNode = insert(TruffleStringBuilder.AppendIntNumberNode.create());
        }
        stringBuilderProfile.append(appendIntNode, sb, value);
    }

    private void append(TruffleStringBuilder sb, long value) {
        if (appendLongNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            appendLongNode = insert(TruffleStringBuilder.AppendLongNumberNode.create());
        }
        stringBuilderProfile.append(appendLongNode, sb, value);
    }

    private TruffleString builderToString(TruffleStringBuilder sb) {
        if (builderToStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            builderToStringNode = insert(TruffleStringBuilder.ToStringNode.create());
        }
        return StringBuilderProfile.toString(builderToStringNode, sb);
    }
}
