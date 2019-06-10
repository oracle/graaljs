/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.DelimitedStringBuilder;

public abstract class JSONStringifyStringNode extends JavaScriptBaseNode {

    private final JSContext context;
    @Child private PropertyGetNode getToJSONProperty;
    @Child private JSFunctionCallNode callToJSONFunction;
    private final BranchProfile sbAppendProfile = BranchProfile.create();

    protected JSONStringifyStringNode(JSContext context) {
        this.context = context;
    }

    public abstract Object execute(Object data, String key, DynamicObject holder);

    public static JSONStringifyStringNode create(JSContext context) {
        return JSONStringifyStringNodeGen.create(context);
    }

    @Specialization
    public Object jsonStrMain(Object jsonData, String key, DynamicObject holder) {
        try {
            assert jsonData instanceof JSONData;
            JSONData data = (JSONData) jsonData;
            Object value = jsonStrPrepare(data, key, holder);
            if (!isStringifyable(value)) {
                return Undefined.instance;
            }
            DelimitedStringBuilder builder = new DelimitedStringBuilder();
            jsonStrExecute(builder, data, value);
            return builder.toString();
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
    private void jsonStrExecute(DelimitedStringBuilder builder, JSONData data, Object value) {
        assert isStringifyable(value);
        if (value == Null.instance) {
            builder.append(Null.NAME, sbAppendProfile);
        } else if (value instanceof Boolean) {
            builder.append((boolean) value ? JSBoolean.TRUE_NAME : JSBoolean.FALSE_NAME, sbAppendProfile);
        } else if (JSRuntime.isString(value)) {
            jsonQuote(builder, value.toString());
        } else if (JSRuntime.isNumber(value)) {
            appendNumber(builder, (Number) value);
        } else if (JSRuntime.isBigInt(value)) {
            throw Errors.createTypeError("Do not know how to serialize a BigInt");
        } else if (JSObject.isJSObject(value) && !JSRuntime.isCallableIsJSObject((DynamicObject) value)) {
            TruffleObject valueObj = (TruffleObject) value;
            if (JSRuntime.isArray(valueObj)) {
                jsonJA(builder, data, valueObj);
            } else {
                jsonJO(builder, data, valueObj);
            }
        } else if (value instanceof TruffleObject) {
            assert JSGuards.isForeignObject(value);
            jsonForeignObject(builder, data, (TruffleObject) value);
        } else if (JSRuntime.isJavaPrimitive(value)) {
            // call toString on Java objects, GR-3722
            jsonQuote(builder, value.toString());
        } else {
            throw new RuntimeException("JSON.stringify: should never reach here, unknown type: " + value + " " + value.getClass());
        }
    }

    private void jsonForeignObject(DelimitedStringBuilder builder, JSONData data, TruffleObject obj) {
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(obj);
        if (interop.isNull(obj)) {
            builder.append(Null.NAME, sbAppendProfile);
        } else if (interop.isBoolean(obj) || interop.isString(obj) || interop.isNumber(obj)) {
            Object unboxed = JSInteropUtil.toPrimitiveOrDefault(obj, Null.instance, interop, this);
            assert !JSGuards.isForeignObject(unboxed);
            jsonStrExecute(builder, data, unboxed);
        } else if (interop.hasArrayElements(obj)) {
            jsonJA(builder, data, obj);
        } else {
            jsonJO(builder, data, obj);
        }
    }

    private void appendNumber(DelimitedStringBuilder builder, Number n) {
        double d = JSRuntime.doubleValue(n);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            builder.append(Null.NAME, sbAppendProfile);
        } else if (n instanceof Integer) {
            builder.append(((Integer) n).intValue(), sbAppendProfile);
        } else if (n instanceof Long) {
            builder.append(((Long) n).longValue(), sbAppendProfile);
        } else {
            builder.append(JSRuntime.doubleToString(d), sbAppendProfile);
        }
    }

    @TruffleBoundary
    private Object jsonStrPrepare(JSONData data, String key, TruffleObject holder) {
        Object value;
        if (JSObject.isJSObject(holder)) {
            value = JSObject.get((DynamicObject) holder, key);
        } else {
            value = truffleRead(holder, key);
        }
        return jsonStrPreparePart2(data, key, holder, value);
    }

    @TruffleBoundary
    private Object jsonStrPrepareArray(JSONData data, int key, DynamicObject holder) {
        Object value = JSObject.get(holder, key);
        return jsonStrPreparePart2(data, key, holder, value);
    }

    @TruffleBoundary
    private Object jsonStrPrepareForeign(JSONData data, int key, TruffleObject holder) {
        assert JSGuards.isForeignObject(holder);
        Object value = truffleRead(holder, key);
        return jsonStrPreparePart2(data, key, holder, value);
    }

    private Object jsonStrPreparePart2(JSONData data, Object key, TruffleObject holder, Object valueArg) {
        Object value = valueArg;
        if (JSRuntime.isObject(value) || JSRuntime.isBigInt(value)) {
            value = jsonStrPrepareObject(JSRuntime.toPropertyKey(key), value);
        }

        if (data.getReplacerFnObj() != null) {
            value = JSRuntime.call(data.getReplacerFnObj(), holder, new Object[]{JSRuntime.toPropertyKey(key), value});
        }
        if (value instanceof TruffleObject) {
            if (JSObject.isJSObject(value)) {
                return jsonStrPrepareJSObject((DynamicObject) value);
            } else if (value instanceof Symbol || (JSRuntime.isForeignObject(value) && InteropLibrary.getFactory().getUncached(value).isExecutable(value))) {
                return Undefined.instance;
            }
        }
        return value;
    }

    private static Object jsonStrPrepareJSObject(DynamicObject valueObj) {
        assert JSObject.isJSObject(valueObj) : "JavaScript object expected, but foreign DynamicObject found";
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
            getToJSONProperty = insert(PropertyGetNode.create("toJSON", false, context));
        }
        Object toJSON = getToJSONProperty.getValue(value);
        if (JSRuntime.isCallable(toJSON)) {
            return jsonStrPrepareObjectFunction(key, value, (DynamicObject) toJSON);
        }
        return value;
    }

    private Object jsonStrPrepareObjectFunction(Object key, Object value, DynamicObject toJSON) {
        if (callToJSONFunction == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callToJSONFunction = insert(JSFunctionCallNode.createCall());
        }
        return callToJSONFunction.executeCall(JSArguments.createOneArg(value, toJSON, key));
    }

    @TruffleBoundary
    private void jsonJO(DelimitedStringBuilder builder, JSONData data, TruffleObject value) {
        checkCycle(data, value);
        data.pushStack(value);
        checkStackDepth(data);
        int stepback = data.getIndent();
        int indent = data.getIndent() + 1;
        data.setIndent(indent);

        concatStart(builder, '{');
        boolean hasContent;
        if (data.getPropertyList() == null) {
            if (JSObject.isJSObject(value)) {
                hasContent = serializeJSONObjectProperties(builder, data, value, indent, JSObject.enumerableOwnNames((DynamicObject) value));
            } else {
                hasContent = serializeForeignObjectProperties(builder, data, value, indent);
            }
        } else {
            hasContent = serializeJSONObjectProperties(builder, data, value, indent, data.getPropertyList());
        }
        concatEnd(builder, data, stepback, '}', hasContent);

        data.popStack();
        data.setIndent(stepback);
    }

    private boolean serializeJSONObjectProperties(DelimitedStringBuilder builder, JSONData data, TruffleObject value, int indent, List<? extends Object> keys) {
        boolean isFirst = true;
        boolean hasContent = false;
        for (Object key : keys) {
            String name = (String) key;
            Object strPPrepared = jsonStrPrepare(data, name, value);
            if (isStringifyable(strPPrepared)) {
                if (isFirst) {
                    concatFirstStep(builder, data);
                    isFirst = false;
                } else {
                    appendSeparator(builder, data, indent);
                }
                jsonQuote(builder, name);
                appendColon(builder, data);
                jsonStrExecute(builder, data, strPPrepared);
                hasContent = true;
            }
        }
        return hasContent;
    }

    private void appendColon(DelimitedStringBuilder builder, JSONData data) {
        builder.append(':', sbAppendProfile);
        if (data.getGap().length() > 0) {
            builder.append(' ', sbAppendProfile);
        }
    }

    private boolean serializeForeignObjectProperties(DelimitedStringBuilder builder, JSONData data, TruffleObject obj, int indent) {
        try {
            InteropLibrary objInterop = InteropLibrary.getFactory().getUncached(obj);
            if (!objInterop.hasMembers(obj)) {
                return false;
            }
            Object keysObj = objInterop.getMembers(obj);
            InteropLibrary keysInterop = InteropLibrary.getFactory().getUncached(keysObj);
            long size = keysInterop.getArraySize(keysObj);
            boolean isFirst = true;
            boolean hasContent = false;
            for (long i = 0; i < size; i++) {
                Object key = keysInterop.readArrayElement(keysObj, i);
                assert InteropLibrary.getFactory().getUncached().isString(key);
                String stringKey = key instanceof String ? (String) key : InteropLibrary.getFactory().getUncached().asString(key);
                if (!objInterop.isMemberReadable(obj, stringKey)) {
                    continue;
                }
                Object memberValue = truffleRead(obj, stringKey);
                Object strPPrepared = jsonStrPreparePart2(data, stringKey, obj, memberValue);
                if (isStringifyable(strPPrepared)) {
                    if (isFirst) {
                        concatFirstStep(builder, data);
                        isFirst = false;
                    } else {
                        appendSeparator(builder, data, indent);
                    }
                    jsonQuote(builder, stringKey);
                    appendColon(builder, data);
                    jsonStrExecute(builder, data, strPPrepared);
                    hasContent = true;
                }
            }
            return hasContent;
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "SerializeJSONObject", this);
        }
    }

    @TruffleBoundary
    private void jsonJA(DelimitedStringBuilder builder, JSONData data, TruffleObject value) {
        checkCycle(data, value);
        assert JSRuntime.isArray(value) || InteropLibrary.getFactory().getUncached().hasArrayElements(value);
        data.pushStack(value);
        checkStackDepth(data);
        int stepback = data.getIndent();
        int indent = data.getIndent() + 1;
        data.setIndent(indent);
        Object lenObject;
        boolean isForeign = false;
        boolean isArray = false;
        if (JSObject.isJSObject(value)) { // Array or Proxy
            lenObject = JSObject.get((DynamicObject) value, JSArray.LENGTH);
            if (JSArray.isJSArray(value)) {
                isArray = true;
            }
        } else {
            lenObject = truffleGetSize(value);
            isForeign = true;
        }
        // output will reach maximum length in at most in StringLengthLimit steps
        long length = JSRuntime.toLength(lenObject);
        if (length > JSTruffleOptions.StringLengthLimit) {
            throw Errors.createRangeErrorInvalidStringLength();
        }
        int len = (int) length;
        concatStart(builder, '[');
        for (int index = 0; index < len; index++) {
            if (index == 0) {
                concatFirstStep(builder, data);
            } else {
                appendSeparator(builder, data, indent);
            }
            Object strPPrepared;
            if (isArray) {
                strPPrepared = jsonStrPrepareArray(data, index, (DynamicObject) value);
            } else if (isForeign) {
                strPPrepared = jsonStrPrepareForeign(data, index, value);
            } else {
                strPPrepared = jsonStrPrepare(data, String.valueOf(index), value);
            }
            if (isStringifyable(strPPrepared)) {
                jsonStrExecute(builder, data, strPPrepared);
            } else {
                builder.append(Null.NAME, sbAppendProfile);
            }
        }

        concatEnd(builder, data, stepback, ']', len > 0);

        data.popStack();
        data.setIndent(stepback);
    }

    private static void checkStackDepth(JSONData data) {
        if (data.stackTooDeep()) {
            throwStackError();
        }
    }

    private static void throwStackError() {
        throw Errors.createRangeError("cannot stringify objects nested that deep");
    }

    private void concatStart(DelimitedStringBuilder builder, char c) {
        builder.append(c, sbAppendProfile);
    }

    private void concatFirstStep(DelimitedStringBuilder builder, JSONData data) {
        if (data.getGap().length() > 0) {
            builder.append('\n', sbAppendProfile);
            for (int i = 0; i < data.getIndent(); i++) {
                builder.append(data.getGap(), sbAppendProfile);
            }
        }
    }

    private void concatEnd(DelimitedStringBuilder builder, JSONData data, int stepback, char close, boolean hasContent) {
        if (data.getGap().length() > 0 && hasContent) {
            builder.append('\n', sbAppendProfile);
            for (int i = 0; i < stepback; i++) {
                builder.append(data.getGap(), sbAppendProfile);
            }
        }
        builder.append(close, sbAppendProfile);
    }

    @TruffleBoundary
    private void appendSeparator(DelimitedStringBuilder builder, JSONData data, int indent) {
        if (data.getGap().length() <= 0) {
            builder.append(',', sbAppendProfile);
        } else {
            builder.append(",\n", sbAppendProfile);
            for (int i = 0; i < indent; i++) {
                builder.append(data.getGap(), sbAppendProfile);
            }
        }
    }

    private static void checkCycle(JSONData data, TruffleObject value) {
        if (data.stack.contains(value)) {
            throw Errors.createTypeError("Converting circular structure to JSON");
        }
    }

    private void jsonQuote(DelimitedStringBuilder builder, String value) {
        builder.append('"', sbAppendProfile);
        for (int i = 0; i < value.length();) {
            char ch = value.charAt(i);
            if (ch < ' ') {
                if (ch == '\b') {
                    builder.append("\\b", sbAppendProfile);
                } else if (ch == '\f') {
                    builder.append("\\f", sbAppendProfile);
                } else if (ch == '\n') {
                    builder.append("\\n", sbAppendProfile);
                } else if (ch == '\r') {
                    builder.append("\\r", sbAppendProfile);
                } else if (ch == '\t') {
                    builder.append("\\t", sbAppendProfile);
                } else {
                    jsonQuoteUnicode(builder, ch);
                }
            } else {
                if (ch == '\\') {
                    builder.append("\\\\", sbAppendProfile);
                } else if (ch == '"') {
                    builder.append("\\\"", sbAppendProfile);
                } else if (Character.isSurrogate(ch)) {
                    if (Character.isHighSurrogate(ch)) {
                        char nextCh;
                        if (i + 1 < value.length() && (Character.isLowSurrogate(nextCh = value.charAt(i + 1)))) {
                            // paired surrogates
                            builder.append(ch, sbAppendProfile);
                            builder.append(nextCh, sbAppendProfile);
                            i++;
                        } else {
                            // unpaired high surrogate
                            jsonQuoteSurrogate(builder, ch);
                        }
                    } else {
                        // unpaired low surrogate
                        jsonQuoteSurrogate(builder, ch);
                    }
                } else {
                    builder.append(ch, sbAppendProfile);
                }
            }
            i++;
        }
        builder.append('"', sbAppendProfile);
    }

    private void jsonQuoteUnicode(DelimitedStringBuilder builder, char c) {
        builder.append("\\u00", sbAppendProfile);
        builder.append(Character.forDigit((c >> 4) & 0xF, 16), sbAppendProfile);
        builder.append(Character.forDigit(c & 0xF, 16), sbAppendProfile);
    }

    private void jsonQuoteSurrogate(DelimitedStringBuilder builder, char c) {
        builder.append("\\ud", sbAppendProfile);
        builder.append(Character.forDigit((c >> 8) & 0xF, 16), sbAppendProfile);
        builder.append(Character.forDigit((c >> 4) & 0xF, 16), sbAppendProfile);
        builder.append(Character.forDigit(c & 0xF, 16), sbAppendProfile);
    }

    private Object truffleGetSize(TruffleObject obj) {
        return JSInteropUtil.getArraySize(obj, InteropLibrary.getFactory().getUncached(), this);
    }

    private Object truffleRead(TruffleObject obj, String key) {
        try {
            return JSRuntime.importValue(InteropLibrary.getFactory().getUncached().readMember(obj, key));
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "readMember", this);
        }
    }

    private Object truffleRead(TruffleObject obj, int index) {
        try {
            return JSRuntime.importValue(InteropLibrary.getFactory().getUncached().readArrayElement(obj, index));
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "readArrayElement", this);
        }
    }
}
