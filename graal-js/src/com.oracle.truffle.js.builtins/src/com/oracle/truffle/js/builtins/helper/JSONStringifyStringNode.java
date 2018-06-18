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
package com.oracle.truffle.js.builtins.helper;

import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.BigInt;
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
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.util.DelimitedStringBuilder;

public abstract class JSONStringifyStringNode extends JavaScriptBaseNode {

    private final JSContext context;
    @Child private PropertyGetNode getToJSONProperty;
    @Child private JSFunctionCallNode callToJSONFunction;
    @Child private Node hasSizeNode;
    @Child private Node getSizeNode;
    @Child private Node readNode;
    @Child private Node keysNode;
    @Child private Node isNullNode;
    @Child private Node isBoxedNode;

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
        return value != Undefined.instance && !JSFunction.isJSFunction(value) && !(value instanceof Symbol);
    }

    @TruffleBoundary
    private void jsonStrExecute(DelimitedStringBuilder builder, JSONData data, Object value) {
        assert isStringifyable(value);
        if (value == Null.instance) {
            builder.append(Null.NAME);
        } else if (value instanceof Boolean) {
            builder.append((boolean) value ? JSBoolean.TRUE_NAME : JSBoolean.FALSE_NAME);
        } else if (JSRuntime.isString(value)) {
            jsonQuote(builder, value.toString());
        } else if (JSRuntime.isNumber(value)) {
            appendNumber(builder, (Number) value);
        } else if (JSRuntime.isBigInt(value)) {
            throw Errors.createTypeError("Do not know how to serialize a BigInt");
        } else if (JSObject.isJSObject(value) && !JSRuntime.isCallable(value)) {
            TruffleObject valueObj = (TruffleObject) value;
            if (JSRuntime.isArray(valueObj)) {
                jsonJA(builder, data, valueObj);
            } else {
                jsonJO(builder, data, valueObj);
            }
        } else if (value instanceof TruffleObject) {
            jsonTruffleObject(builder, data, (TruffleObject) value);
        } else if (JSTruffleOptions.NashornJavaInterop || JSRuntime.isJavaPrimitive(value)) {
            // call toString on Java objects, GR-3722
            jsonQuote(builder, value.toString());
        } else {
            throw new RuntimeException("JSON.stringify: should never reach here, unknown type: " + value + " " + value.getClass());
        }
    }

    private void jsonTruffleObject(DelimitedStringBuilder builder, JSONData data, TruffleObject obj) {
        if (truffleIsNull(obj)) {
            builder.append(Null.NAME);
        } else if (truffleIsBoxed(obj)) {
            jsonStrExecute(builder, data, JSInteropNodeUtil.unbox(obj));
        } else if (truffleHasSize(obj)) {
            jsonJA(builder, data, obj);
        } else {
            jsonJO(builder, data, obj);
        }
    }

    private static void appendNumber(DelimitedStringBuilder builder, Number n) {
        double d = JSRuntime.doubleValue(n);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            builder.append(Null.NAME);
        } else if (n instanceof Integer) {
            builder.append(((Integer) n).intValue());
        } else if (n instanceof Long) {
            builder.append(((Long) n).longValue());
        } else {
            builder.append(JSRuntime.doubleToString(d));
        }
    }

    @TruffleBoundary
    /**
     * Generic case, for Proxies.
     */
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
        if (JSRuntime.isObject(value)) {
            DynamicObject valueObj = (DynamicObject) value;
            value = jsonStrPrepareObject(JSRuntime.toPropertyKey(key), value, valueObj);
        } else if (JSRuntime.isBigInt(value)) {
            DynamicObject valueObj = JSBigInt.create(this.context, (BigInt) value);
            value = jsonStrPrepareObject(JSRuntime.toPropertyKey(key), value, valueObj);
        }

        if (data.getReplacerFnObj() != null) {
            value = JSRuntime.call(data.getReplacerFnObj(), holder, new Object[]{JSRuntime.toPropertyKey(key), value});
        }
        if (JSObject.isJSObject(value)) {
            return jsonStrPrepareJSObject((DynamicObject) value);
        } else {
            return value;
        }
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
        } else if (JSRuntime.isCallable(valueObj)) {
            return Undefined.instance;
        }
        return valueObj;
    }

    private Object jsonStrPrepareObject(Object key, Object value, DynamicObject valueObj) {
        assert JSRuntime.isPropertyKey(key);
        if (getToJSONProperty == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getToJSONProperty = insert(PropertyGetNode.create("toJSON", false, context));
        }
        Object toJSON = getToJSONProperty.getValue(valueObj);
        if (JSFunction.isJSFunction(toJSON)) {
            return jsonStrPrepareObjectFunction(key, valueObj, (DynamicObject) toJSON);
        }
        return value;
    }

    private Object jsonStrPrepareObjectFunction(Object key, DynamicObject valueObj, DynamicObject toJSON) {
        if (callToJSONFunction == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callToJSONFunction = insert(JSFunctionCallNode.createCall());
        }
        return callToJSONFunction.executeCall(JSArguments.createOneArg(valueObj, toJSON, key));
    }

    @TruffleBoundary
    private void jsonJO(DelimitedStringBuilder builder, JSONData data, TruffleObject value) {
        checkCycle(data, value);
        data.pushStack(value);
        checkStackDepth(data);
        int stepback = data.getIndent();
        int indent = data.getIndent() + 1;
        data.setIndent(indent);
        List<? extends Object> keys;
        if (data.getPropertyList() == null) {
            if (JSObject.isJSObject(value)) {
                keys = JSObject.enumerableOwnNames((DynamicObject) value);
            } else {
                keys = truffleKeys(value);
            }
        } else {
            keys = data.getPropertyList();
        }
        boolean isFirst = true;
        boolean hasContent = false;

        concatStart(builder, '{');
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
                builder.append(':');
                if (data.getGap().length() > 0) {
                    builder.append(' ');
                }
                jsonStrExecute(builder, data, strPPrepared);
                hasContent = true;
            }
        }
        concatEnd(builder, data, stepback, '}', hasContent);

        data.popStack();
        data.setIndent(stepback);

    }

    @TruffleBoundary
    private void jsonJA(DelimitedStringBuilder builder, JSONData data, TruffleObject value) {
        checkCycle(data, value);
        assert JSRuntime.isArray(value) || truffleHasSize(value);
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
                builder.append(Null.NAME);
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

    private static void concatStart(DelimitedStringBuilder builder, char c) {
        builder.append(c);
    }

    private static void concatFirstStep(DelimitedStringBuilder builder, JSONData data) {
        if (data.getGap().length() > 0) {
            builder.append('\n');
            for (int i = 0; i < data.getIndent(); i++) {
                builder.append(data.getGap());
            }
        }
    }

    private static void concatEnd(DelimitedStringBuilder builder, JSONData data, int stepback, char close, boolean hasContent) {
        if (data.getGap().length() > 0 && hasContent) {
            builder.append('\n');
            for (int i = 0; i < stepback; i++) {
                builder.append(data.getGap());
            }
        }
        builder.append(close);
    }

    @TruffleBoundary
    private static void appendSeparator(DelimitedStringBuilder builder, JSONData data, int indent) {
        if (data.getGap().length() <= 0) {
            builder.append(',');
        } else {
            builder.append(",\n");
            for (int i = 0; i < indent; i++) {
                builder.append(data.getGap());
            }
        }
    }

    private static void checkCycle(JSONData data, TruffleObject value) {
        if (data.stack.contains(value)) {
            throw Errors.createTypeError("Converting circular structure to JSON");
        }
    }

    private static void jsonQuote(DelimitedStringBuilder builder, String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < ' ') {
                if (ch == '\b') {
                    builder.append("\\b");
                } else if (ch == '\f') {
                    builder.append("\\f");
                } else if (ch == '\n') {
                    builder.append("\\n");
                } else if (ch == '\r') {
                    builder.append("\\r");
                } else if (ch == '\t') {
                    builder.append("\\t");
                } else {
                    jsonQuoteUnicode(builder, ch);
                }
            } else {
                if (ch == '\\') {
                    builder.append("\\\\");
                } else if (ch == '"') {
                    builder.append("\\\"");
                } else {
                    builder.append(ch);
                }
            }
        }
        builder.append('"');
    }

    private static void jsonQuoteUnicode(DelimitedStringBuilder builder, char c) {
        builder.append("\\u00");
        builder.append(Character.forDigit((c >> 4) & 0xF, 16));
        builder.append(Character.forDigit(c & 0xF, 16));
    }

    private boolean truffleHasSize(TruffleObject obj) {
        CompilerAsserts.neverPartOfCompilation("JSONStringifyStringNode.truffleHasSize");
        if (hasSizeNode == null) {
            hasSizeNode = insert(Message.HAS_SIZE.createNode());
        }
        return ForeignAccess.sendHasSize(hasSizeNode, obj);
    }

    private Object truffleGetSize(TruffleObject obj) {
        CompilerAsserts.neverPartOfCompilation("JSONStringifyStringNode.truffleGetSize");
        if (getSizeNode == null) {
            getSizeNode = insert(Message.GET_SIZE.createNode());
        }
        return JSInteropNodeUtil.getSize(obj, getSizeNode);
    }

    private Object truffleRead(TruffleObject obj, Object key) {
        CompilerAsserts.neverPartOfCompilation("JSONStringifyStringNode.truffleRead");
        if (readNode == null) {
            readNode = insert(Message.READ.createNode());
        }
        return JSRuntime.importValue(JSInteropNodeUtil.read(obj, key, readNode));
    }

    private List<Object> truffleKeys(TruffleObject obj) {
        CompilerAsserts.neverPartOfCompilation("JSONStringifyStringNode.truffleKeys");
        if (keysNode == null) {
            keysNode = insert(Message.KEYS.createNode());
        }
        if (readNode == null) {
            readNode = insert(Message.READ.createNode());
        }
        if (getSizeNode == null) {
            getSizeNode = insert(Message.GET_SIZE.createNode());
        }
        return JSInteropNodeUtil.keys(obj, keysNode, readNode, getSizeNode, true);
    }

    private boolean truffleIsNull(TruffleObject obj) {
        if (isNullNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isNullNode = insert(Message.IS_NULL.createNode());
        }
        return ForeignAccess.sendIsNull(isNullNode, obj);
    }

    private boolean truffleIsBoxed(TruffleObject obj) {
        if (isBoxedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isBoxedNode = insert(Message.IS_BOXED.createNode());
        }
        return ForeignAccess.sendIsBoxed(isBoxedNode, obj);
    }
}
