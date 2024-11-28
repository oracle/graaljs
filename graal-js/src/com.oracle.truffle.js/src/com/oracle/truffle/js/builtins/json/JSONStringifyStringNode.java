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

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSRawJSONObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;

public abstract class JSONStringifyStringNode extends JavaScriptBaseNode {

    @Child private PropertyGetNode getToJSONProperty;
    @Child private JSFunctionCallNode callToJSONFunction;

    @Child private TruffleStringBuilder.ToStringNode builderToStringNode = TruffleStringBuilder.ToStringNode.create();

    private final StringBuilderProfile stringBuilderProfile;

    protected JSONStringifyStringNode(JSContext context) {
        this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
    }

    public abstract Object execute(Object data, Object keyStr, JSObject holder);

    @NeverDefault
    public static JSONStringifyStringNode create(JSContext context) {
        return JSONStringifyStringNodeGen.create(context);
    }

    @Specialization
    public Object jsonStrMain(Object jsonData, TruffleString keyStr, JSObject holder) {
        try {
            assert jsonData instanceof JSONData;
            JSONData data = (JSONData) jsonData;
            Object value = getPreparedJSONPropertyValue(data, keyStr, holder);
            if (!isStringifyable(value)) {
                return Undefined.instance;
            }
            var sb = stringBuilderProfile.newStringBuilder();
            serializeJSONPropertyValue(sb, data, value);
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

    /**
     * The last part of SerializeJSONProperty, serializing the value.
     *
     * @see #prepareJSONPropertyValue
     */
    @TruffleBoundary
    private void serializeJSONPropertyValue(TruffleStringBuilderUTF16 builder, JSONData data, Object value) {
        assert isStringifyable(value);
        if (value == Null.instance) {
            append(builder, Null.NAME);
        } else if (value instanceof Boolean) {
            appendBoolean(builder, (boolean) value);
        } else if (value instanceof TruffleString str) {
            jsonQuote(builder, str);
        } else if (JSRuntime.isNumber(value)) {
            appendNumber(builder, value);
        } else if (JSObject.isJSObject(value)) {
            JSObject valueObj = (JSObject) value;
            assert !JSRuntime.isCallableIsJSObject(valueObj);
            if (valueObj instanceof JSRawJSONObject rawJSONObject) {
                append(builder, rawJSONObject.getRawJSON());
            } else if (JSRuntime.isArray(valueObj)) {
                serializeJSONArray(builder, data, valueObj);
            } else {
                serializeJSONObject(builder, data, valueObj);
            }
        } else if (JSRuntime.isBigInt(value)) {
            throw Errors.createTypeError("Do not know how to serialize a BigInt");
        } else if (value instanceof TruffleObject || value instanceof Long) {
            serializeForeignObject(builder, data, value);
        } else {
            throw unsupportedType(value);
        }
    }

    @TruffleBoundary
    private static RuntimeException unsupportedType(Object value) {
        assert false : "JSON.stringify: should never reach here, unknown type: " + value + " " + value.getClass();
        return Errors.createTypeError("Do not know how to serialize a value of type " + (value == null ? "null" : value.getClass().getTypeName()));
    }

    private void serializeForeignObject(TruffleStringBuilderUTF16 sb, JSONData data, Object obj) {
        assert JSGuards.isForeignObjectOrNumber(obj);
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(obj);
        if (interop.isNull(obj)) {
            append(sb, Null.NAME);
            return;
        }
        try {
            if (interop.isBoolean(obj)) {
                appendBoolean(sb, interop.asBoolean(obj));
            } else if (interop.isString(obj)) {
                jsonQuote(sb, interop.asTruffleString(obj));
            } else if (interop.isNumber(obj)) {
                if (interop.fitsInInt(obj)) {
                    appendNumber(sb, interop.asInt(obj));
                } else if (interop.fitsInDouble(obj)) {
                    appendNumber(sb, interop.asDouble(obj));
                } else {
                    throw Errors.createTypeError("Do not know how to serialize a BigInt");
                }
            } else if (interop.hasArrayElements(obj)) {
                serializeJSONArray(sb, data, obj);
            } else {
                serializeJSONObject(sb, data, obj);
            }
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorUnboxException(obj, e, this);
        }
    }

    private void appendBoolean(TruffleStringBuilderUTF16 builder, boolean value) {
        append(builder, value ? JSBoolean.TRUE_NAME : JSBoolean.FALSE_NAME);
    }

    private void appendNumber(TruffleStringBuilderUTF16 builder, Object number) {
        assert number instanceof Number;
        if (number instanceof Integer) {
            append(builder, ((Integer) number).intValue());
        } else if (number instanceof SafeInteger) {
            append(builder, ((SafeInteger) number).longValue());
        } else {
            double d;
            if (number instanceof Double) {
                d = ((Double) number).doubleValue();
            } else {
                d = JSRuntime.doubleValue((Number) number);
            }
            TruffleString str;
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                str = Null.NAME;
            } else {
                str = JSRuntime.doubleToString(d);
            }
            append(builder, str);
        }
    }

    /**
     * The first parts of SerializeJSONProperty, getting the property value from the object and
     * preparing it for serialization.
     */
    @TruffleBoundary
    private Object getPreparedJSONPropertyValue(JSONData data, TruffleString keyStr, Object holder) {
        Object value;
        if (JSObject.isJSObject(holder)) {
            value = JSObject.get((JSObject) holder, keyStr);
        } else {
            value = truffleRead(holder, keyStr);
        }
        return prepareJSONPropertyValue(data, keyStr, holder, value);
    }

    @TruffleBoundary
    private Object getPreparedJSONPropertyValueFromJSArray(JSONData data, int key, JSObject holder) {
        Object value = JSObject.get(holder, key);
        return prepareJSONPropertyValue(data, Strings.fromInt(key), holder, value);
    }

    @TruffleBoundary
    private Object getPreparedJSONPropertyValueFromForeignArray(JSONData data, int key, Object holder) {
        assert JSGuards.isForeignObject(holder);
        Object value = truffleRead(holder, key);
        return prepareJSONPropertyValue(data, Strings.fromInt(key), holder, value);
    }

    /**
     * The second part of SerializeJSONProperty, preparing the property value for serialization (by
     * calling toJSON(), the replacer function, and/or unboxing).
     *
     * @see #serializeJSONPropertyValue
     */
    private Object prepareJSONPropertyValue(JSONData data, Object key, Object holder, Object valueArg) {
        Object value = valueArg;
        boolean tryToJSON = false;
        if (JSRuntime.isObject(value) || JSRuntime.isBigInt(value)) {
            tryToJSON = true;
        } else if (JSRuntime.isForeignObject(value)) {
            InteropLibrary interop = InteropLibrary.getUncached(value);
            tryToJSON = interop.hasMembers(value) && !interop.isNull(value) && !JSInteropUtil.isBoxedPrimitive(value, interop);
        }
        if (tryToJSON) {
            value = tryToJSONMethod(key, value);
        }

        if (data.getReplacerFnObj() != null) {
            value = JSRuntime.call(data.getReplacerFnObj(), holder, new Object[]{key, value});
        }
        if (JSObject.isJSObject(value)) {
            return prepareJSObject((JSObject) value);
        } else if (value instanceof Symbol) {
            return Undefined.instance;
        } else if (JSRuntime.isCallableForeign(value)) {
            return Undefined.instance;
        }
        return value;
    }

    private static Object prepareJSObject(JSObject valueObj) {
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

    private Object tryToJSONMethod(Object key, Object value) {
        assert JSRuntime.isPropertyKey(key);
        if (getToJSONProperty == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getToJSONProperty = insert(PropertyGetNode.create(Strings.TO_JSON, getJSContext()));
        }
        Object toJSON = getToJSONProperty.getValue(value);
        if (JSRuntime.isCallable(toJSON)) {
            return callToJSONMethod(key, value, toJSON);
        }
        return value;
    }

    private Object callToJSONMethod(Object key, Object value, Object toJSON) {
        if (callToJSONFunction == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callToJSONFunction = insert(JSFunctionCallNode.createCall());
        }
        return callToJSONFunction.executeCall(JSArguments.createOneArg(value, toJSON, key));
    }

    @TruffleBoundary
    private TruffleStringBuilder serializeJSONObject(TruffleStringBuilderUTF16 sb, JSONData data, Object value) {
        assert !JSRuntime.isNullish(value) : value;
        checkCycle(data, value);
        data.pushStack(value);
        checkStackDepth(data);
        int stepback = data.getIndent();
        int indent = data.getIndent() + 1;
        data.setIndent(indent);

        concatStart(sb, '{');
        int lengthBefore = StringBuilderProfile.length(sb);
        if (data.getPropertyList() == null) {
            if (JSObject.isJSObject(value)) {
                serializeJSONObjectProperties(sb, data, value, indent, JSObject.enumerableOwnNames((JSObject) value));
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

    private TruffleStringBuilder serializeJSONObjectProperties(TruffleStringBuilderUTF16 sb, JSONData data, Object value, int indent, List<? extends Object> keys) {
        boolean isFirst = true;
        for (Object key : keys) {
            TruffleString name = (TruffleString) key;
            Object strPPrepared = getPreparedJSONPropertyValue(data, name, value);
            if (isStringifyable(strPPrepared)) {
                if (isFirst) {
                    concatFirstStep(sb, data);
                    isFirst = false;
                } else {
                    appendSeparator(sb, data, indent);
                }
                jsonQuote(sb, name);
                appendColon(sb, data);
                serializeJSONPropertyValue(sb, data, strPPrepared);
            }
        }
        return sb;
    }

    private void appendColon(TruffleStringBuilderUTF16 sb, JSONData data) {
        append(sb, ':');
        if (Strings.length(data.getGap()) > 0) {
            append(sb, ' ');
        }
    }

    private void serializeForeignObjectProperties(TruffleStringBuilderUTF16 sb, JSONData data, Object obj, int indent) {
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
                TruffleString stringKey = Strings.interopAsTruffleString(key);
                if (!objInterop.isMemberReadable(obj, Strings.toJavaString(stringKey))) {
                    continue;
                }
                Object memberValue = truffleRead(obj, stringKey);
                Object strPPrepared = prepareJSONPropertyValue(data, stringKey, obj, memberValue);
                if (isStringifyable(strPPrepared)) {
                    if (isFirst) {
                        concatFirstStep(sb, data);
                        isFirst = false;
                    } else {
                        appendSeparator(sb, data, indent);
                    }
                    jsonQuote(sb, stringKey);
                    appendColon(sb, data);
                    serializeJSONPropertyValue(sb, data, strPPrepared);
                }
            }
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "SerializeJSONObject", this);
        }
    }

    @TruffleBoundary
    private TruffleStringBuilder serializeJSONArray(TruffleStringBuilderUTF16 sb, JSONData data, Object value) {
        assert JSRuntime.isArray(value) : value;
        checkCycle(data, value);
        data.pushStack(value);
        checkStackDepth(data);
        int stepback = data.getIndent();
        int indent = data.getIndent() + 1;
        data.setIndent(indent);
        Object lenObject;
        boolean isForeign = false;
        boolean isArray = false;
        if (JSObject.isJSObject(value)) { // Array or Proxy
            lenObject = JSObject.get((JSObject) value, JSArray.LENGTH);
            if (JSArray.isJSArray(value)) {
                isArray = true;
            }
        } else {
            lenObject = truffleGetSize(value);
            isForeign = true;
        }
        // output will reach maximum length in at most in StringLengthLimit steps
        long length = JSRuntime.toLength(lenObject);
        if (length > stringBuilderProfile.getStringLengthLimit()) {
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
                strPPrepared = getPreparedJSONPropertyValueFromJSArray(data, index, (JSObject) value);
            } else if (isForeign) {
                strPPrepared = getPreparedJSONPropertyValueFromForeignArray(data, index, value);
            } else {
                strPPrepared = getPreparedJSONPropertyValue(data, Strings.fromInt(index), value);
            }
            if (isStringifyable(strPPrepared)) {
                serializeJSONPropertyValue(sb, data, strPPrepared);
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

    private void concatStart(TruffleStringBuilderUTF16 builder, char c) {
        append(builder, c);
    }

    private void concatFirstStep(TruffleStringBuilderUTF16 sb, JSONData data) {
        if (Strings.length(data.getGap()) > 0) {
            append(sb, '\n');
            for (int i = 0; i < data.getIndent(); i++) {
                append(sb, data.getGap());
            }
        }
    }

    private void concatEnd(TruffleStringBuilderUTF16 sb, JSONData data, int stepback, char close, boolean hasContent) {
        if (Strings.length(data.getGap()) > 0 && hasContent) {
            append(sb, '\n');
            for (int i = 0; i < stepback; i++) {
                append(sb, data.getGap());
            }
        }
        append(sb, close);
    }

    @TruffleBoundary
    private void appendSeparator(TruffleStringBuilderUTF16 sb, JSONData data, int indent) {
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

    private TruffleStringBuilder jsonQuote(TruffleStringBuilderUTF16 builder, TruffleString valueStr) {
        return jsonQuote(stringBuilderProfile, builder, valueStr,
                        TruffleString.ReadCharUTF16Node.getUncached(),
                        TruffleStringBuilder.AppendCharUTF16Node.getUncached(),
                        TruffleStringBuilder.AppendStringNode.getUncached(),
                        TruffleStringBuilder.AppendSubstringByteIndexNode.getUncached());
    }

    public static TruffleStringBuilder jsonQuote(StringBuilderProfile stringBuilderProfile, TruffleStringBuilderUTF16 sb, TruffleString valueStr,
                    TruffleString.ReadCharUTF16Node readCharNode,
                    TruffleStringBuilder.AppendCharUTF16Node appendCharNode,
                    TruffleStringBuilder.AppendStringNode appendStringNode,
                    TruffleStringBuilder.AppendSubstringByteIndexNode appendSubstringNode) {
        stringBuilderProfile.append(appendCharNode, sb, '"');
        int length = Strings.length(valueStr);
        int pos = 0;
        if (valueStr.getCodeRangeImpreciseUncached(TruffleString.Encoding.UTF_16).isSubsetOf(TruffleString.CodeRange.BMP)) {
            // BMP: No surrogate handling necessary.
            // Check if any escaping is needed (control, quote, or backslash).
            for (; pos < length; pos++) {
                char ch = Strings.charAt(readCharNode, valueStr, pos);
                if (ch < ' ' || ch == '"' || ch == '\\') {
                    break;
                } else {
                    assert !Character.isSurrogate(ch);
                }
            }
            if (pos == length) {
                stringBuilderProfile.append(appendStringNode, sb, valueStr);
            } else if (pos > 0) {
                stringBuilderProfile.appendLen(appendSubstringNode, sb, valueStr, 0, pos);
            }
        }
        if (pos < length) {
            jsonQuoteWithEscape(stringBuilderProfile, sb, valueStr, pos,
                            readCharNode, appendCharNode, appendStringNode);
        }
        stringBuilderProfile.append(appendCharNode, sb, '"');
        return sb;
    }

    private static void jsonQuoteWithEscape(StringBuilderProfile stringBuilderProfile, TruffleStringBuilderUTF16 sb, TruffleString valueStr, int startPos,
                    TruffleString.ReadCharUTF16Node readCharNode,
                    TruffleStringBuilder.AppendCharUTF16Node appendCharNode,
                    TruffleStringBuilder.AppendStringNode appendStringNode) {
        int length = Strings.length(valueStr);
        int i = startPos;
        for (; i < length; i++) {
            char ch = Strings.charAt(readCharNode, valueStr, i);
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
                        if (i + 1 < length && (Character.isLowSurrogate(nextCh = Strings.charAt(readCharNode, valueStr, i + 1)))) {
                            // paired surrogates
                            stringBuilderProfile.append(appendCharNode, sb, ch);
                            stringBuilderProfile.append(appendCharNode, sb, nextCh);
                            i++;
                            continue;
                        }
                        // unpaired high surrogate
                    } else {
                        // unpaired low surrogate
                        assert Character.isLowSurrogate(ch);
                    }
                    jsonQuoteSurrogate(stringBuilderProfile, sb, ch, appendCharNode, appendStringNode);
                } else {
                    stringBuilderProfile.append(appendCharNode, sb, ch);
                }
            }
        }
    }

    private static void jsonQuoteUnicode(StringBuilderProfile stringBuilderProfile, TruffleStringBuilderUTF16 sb, char c,
                    TruffleStringBuilder.AppendCharUTF16Node appendCharNode,
                    TruffleStringBuilder.AppendStringNode appendStringNode) {
        stringBuilderProfile.append(appendStringNode, sb, Strings.BACKSLASH_U00);
        stringBuilderProfile.append(appendCharNode, sb, Character.forDigit((c >> 4) & 0xF, 16));
        stringBuilderProfile.append(appendCharNode, sb, Character.forDigit(c & 0xF, 16));
    }

    private static void jsonQuoteSurrogate(StringBuilderProfile stringBuilderProfile, TruffleStringBuilderUTF16 sb, char c,
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

    private static Object truffleRead(Object obj, TruffleString keyStr) {
        return JSInteropUtil.readMemberOrDefault(obj, keyStr, Undefined.instance);
    }

    private static Object truffleRead(Object obj, int index) {
        return JSInteropUtil.readArrayElementOrDefault(obj, index, Undefined.instance);
    }

    private void append(TruffleStringBuilderUTF16 sb, TruffleString s) {
        stringBuilderProfile.append(TruffleStringBuilder.AppendStringNode.getUncached(), sb, s);
    }

    private void append(TruffleStringBuilderUTF16 sb, char value) {
        stringBuilderProfile.append(TruffleStringBuilder.AppendCharUTF16Node.getUncached(), sb, value);
    }

    private void append(TruffleStringBuilderUTF16 sb, int value) {
        stringBuilderProfile.append(TruffleStringBuilder.AppendIntNumberNode.getUncached(), sb, value);
    }

    private void append(TruffleStringBuilderUTF16 sb, long value) {
        stringBuilderProfile.append(TruffleStringBuilder.AppendLongNumberNode.getUncached(), sb, value);
    }

    private TruffleString builderToString(TruffleStringBuilderUTF16 sb) {
        return StringBuilderProfile.toString(builderToStringNode, sb);
    }
}
