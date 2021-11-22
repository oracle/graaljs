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
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
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
    private final StringBuilderProfile stringBuilderProfile;

    protected JSONStringifyStringNode(JSContext context) {
        this.context = context;
        this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
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
            StringBuilder builder = new StringBuilder();
            jsonStrExecute(builder, data, value);
            return stringBuilderProfile.toString(builder);
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
    private void jsonStrExecute(StringBuilder builder, JSONData data, Object value) {
        assert isStringifyable(value);
        if (value == Null.instance) {
            stringBuilderProfile.append(builder, Null.NAME);
        } else if (value instanceof Boolean) {
            stringBuilderProfile.append(builder, (boolean) value ? JSBoolean.TRUE_NAME : JSBoolean.FALSE_NAME);
        } else if (JSRuntime.isString(value)) {
            jsonQuote(stringBuilderProfile, builder, value.toString());
        } else if (JSRuntime.isNumber(value)) {
            appendNumber(builder, (Number) value);
        } else if (JSRuntime.isBigInt(value)) {
            throw Errors.createTypeError("Do not know how to serialize a BigInt");
        } else if (JSDynamicObject.isJSDynamicObject(value) && !JSRuntime.isCallableIsJSObject((DynamicObject) value)) {
            DynamicObject valueObj = (DynamicObject) value;
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
            jsonQuote(stringBuilderProfile, builder, value.toString());
        } else {
            throw new RuntimeException("JSON.stringify: should never reach here, unknown type: " + value + " " + value.getClass());
        }
    }

    private void jsonForeignObject(StringBuilder builder, JSONData data, Object obj) {
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(obj);
        if (interop.isNull(obj)) {
            stringBuilderProfile.append(builder, Null.NAME);
        } else if (JSInteropUtil.isBoxedPrimitive(obj, interop)) {
            Object unboxed = JSInteropUtil.toPrimitiveOrDefault(obj, Null.instance, interop, this);
            assert !JSGuards.isForeignObject(unboxed);
            jsonStrExecute(builder, data, unboxed);
        } else if (interop.hasArrayElements(obj)) {
            jsonJA(builder, data, obj);
        } else {
            jsonJO(builder, data, obj);
        }
    }

    private void appendNumber(StringBuilder builder, Number n) {
        double d = JSRuntime.doubleValue(n);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            stringBuilderProfile.append(builder, Null.NAME);
        } else if (n instanceof Integer) {
            stringBuilderProfile.append(builder, ((Integer) n).intValue());
        } else if (n instanceof Long) {
            stringBuilderProfile.append(builder, ((Long) n).longValue());
        } else {
            stringBuilderProfile.append(builder, JSRuntime.doubleToString(d));
        }
    }

    @TruffleBoundary
    private Object jsonStrPrepare(JSONData data, String key, Object holder) {
        Object value;
        if (JSDynamicObject.isJSDynamicObject(holder)) {
            value = JSObject.get((DynamicObject) holder, key);
        } else {
            value = truffleRead(holder, key);
        }
        return jsonStrPreparePart2(data, key, holder, value);
    }

    @TruffleBoundary
    private Object jsonStrPrepareArray(JSONData data, int key, DynamicObject holder) {
        Object value = JSObject.get(holder, key);
        return jsonStrPreparePart2(data, String.valueOf(key), holder, value);
    }

    @TruffleBoundary
    private Object jsonStrPrepareForeign(JSONData data, int key, Object holder) {
        assert JSGuards.isForeignObject(holder);
        Object value = truffleRead(holder, key);
        return jsonStrPreparePart2(data, String.valueOf(key), holder, value);
    }

    private Object jsonStrPreparePart2(JSONData data, String key, Object holder, Object valueArg) {
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
            return jsonStrPrepareJSObject((DynamicObject) value);
        } else if (value instanceof Symbol) {
            return Undefined.instance;
        } else if (JSRuntime.isCallableForeign(value)) {
            return Undefined.instance;
        }
        return value;
    }

    private static Object jsonStrPrepareJSObject(DynamicObject valueObj) {
        assert JSDynamicObject.isJSDynamicObject(valueObj) : "JavaScript object expected, but foreign DynamicObject found";
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
    private void jsonJO(StringBuilder builder, JSONData data, Object value) {
        checkCycle(data, value);
        data.pushStack(value);
        checkStackDepth(data);
        int stepback = data.getIndent();
        int indent = data.getIndent() + 1;
        data.setIndent(indent);

        concatStart(builder, '{');
        boolean hasContent;
        if (data.getPropertyList() == null) {
            if (JSDynamicObject.isJSDynamicObject(value)) {
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

    private boolean serializeJSONObjectProperties(StringBuilder builder, JSONData data, Object value, int indent, List<? extends Object> keys) {
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
                jsonQuote(stringBuilderProfile, builder, name);
                appendColon(builder, data);
                jsonStrExecute(builder, data, strPPrepared);
                hasContent = true;
            }
        }
        return hasContent;
    }

    private void appendColon(StringBuilder builder, JSONData data) {
        stringBuilderProfile.append(builder, ':');
        if (data.getGap().length() > 0) {
            stringBuilderProfile.append(builder, ' ');
        }
    }

    private boolean serializeForeignObjectProperties(StringBuilder builder, JSONData data, Object obj, int indent) {
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
                assert InteropLibrary.getUncached().isString(key);
                String stringKey = key instanceof String ? (String) key : InteropLibrary.getUncached().asString(key);
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
                    jsonQuote(stringBuilderProfile, builder, stringKey);
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
    private void jsonJA(StringBuilder builder, JSONData data, Object value) {
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
        if (length > context.getStringLengthLimit()) {
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
                stringBuilderProfile.append(builder, Null.NAME);
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

    private void concatStart(StringBuilder builder, char c) {
        stringBuilderProfile.append(builder, c);
    }

    private void concatFirstStep(StringBuilder builder, JSONData data) {
        if (data.getGap().length() > 0) {
            stringBuilderProfile.append(builder, '\n');
            for (int i = 0; i < data.getIndent(); i++) {
                stringBuilderProfile.append(builder, data.getGap());
            }
        }
    }

    private void concatEnd(StringBuilder builder, JSONData data, int stepback, char close, boolean hasContent) {
        if (data.getGap().length() > 0 && hasContent) {
            stringBuilderProfile.append(builder, '\n');
            for (int i = 0; i < stepback; i++) {
                stringBuilderProfile.append(builder, data.getGap());
            }
        }
        stringBuilderProfile.append(builder, close);
    }

    @TruffleBoundary
    private void appendSeparator(StringBuilder builder, JSONData data, int indent) {
        if (data.getGap().length() <= 0) {
            stringBuilderProfile.append(builder, ',');
        } else {
            stringBuilderProfile.append(builder, ",\n");
            for (int i = 0; i < indent; i++) {
                stringBuilderProfile.append(builder, data.getGap());
            }
        }
    }

    private static void checkCycle(JSONData data, Object value) {
        if (data.stack.contains(value)) {
            throw Errors.createTypeError("Converting circular structure to JSON");
        }
    }

    @TruffleBoundary
    public static void jsonQuote(StringBuilderProfile stringBuilderProfile, StringBuilder builder, String value) {
        stringBuilderProfile.append(builder, '"');
        for (int i = 0; i < value.length();) {
            char ch = value.charAt(i);
            if (ch < ' ') {
                if (ch == '\b') {
                    stringBuilderProfile.append(builder, "\\b");
                } else if (ch == '\f') {
                    stringBuilderProfile.append(builder, "\\f");
                } else if (ch == '\n') {
                    stringBuilderProfile.append(builder, "\\n");
                } else if (ch == '\r') {
                    stringBuilderProfile.append(builder, "\\r");
                } else if (ch == '\t') {
                    stringBuilderProfile.append(builder, "\\t");
                } else {
                    jsonQuoteUnicode(stringBuilderProfile, builder, ch);
                }
            } else {
                if (ch == '\\') {
                    stringBuilderProfile.append(builder, "\\\\");
                } else if (ch == '"') {
                    stringBuilderProfile.append(builder, "\\\"");
                } else if (Character.isSurrogate(ch)) {
                    if (Character.isHighSurrogate(ch)) {
                        char nextCh;
                        if (i + 1 < value.length() && (Character.isLowSurrogate(nextCh = value.charAt(i + 1)))) {
                            // paired surrogates
                            stringBuilderProfile.append(builder, ch);
                            stringBuilderProfile.append(builder, nextCh);
                            i++;
                        } else {
                            // unpaired high surrogate
                            jsonQuoteSurrogate(stringBuilderProfile, builder, ch);
                        }
                    } else {
                        // unpaired low surrogate
                        jsonQuoteSurrogate(stringBuilderProfile, builder, ch);
                    }
                } else {
                    stringBuilderProfile.append(builder, ch);
                }
            }
            i++;
        }
        stringBuilderProfile.append(builder, '"');
    }

    private static void jsonQuoteUnicode(StringBuilderProfile profile, StringBuilder builder, char c) {
        profile.append(builder, "\\u00");
        profile.append(builder, Character.forDigit((c >> 4) & 0xF, 16));
        profile.append(builder, Character.forDigit(c & 0xF, 16));
    }

    private static void jsonQuoteSurrogate(StringBuilderProfile profile, StringBuilder builder, char c) {
        profile.append(builder, "\\ud");
        profile.append(builder, Character.forDigit((c >> 8) & 0xF, 16));
        profile.append(builder, Character.forDigit((c >> 4) & 0xF, 16));
        profile.append(builder, Character.forDigit(c & 0xF, 16));
    }

    private Object truffleGetSize(Object obj) {
        return JSInteropUtil.getArraySize(obj, InteropLibrary.getUncached(), this);
    }

    private Object truffleRead(Object obj, String key) {
        try {
            return JSRuntime.importValue(InteropLibrary.getUncached().readMember(obj, key));
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "readMember", key, this);
        }
    }

    private Object truffleRead(Object obj, int index) {
        try {
            return JSRuntime.importValue(InteropLibrary.getUncached().readArrayElement(obj, index));
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw Errors.createTypeErrorInteropException(obj, e, "readArrayElement", index, this);
        }
    }
}
