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
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNodeGen;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.util.IntlUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ECMA intl402 9.2.1 "CanonicalizeLocaleList" as Truffle node.
 * https://tc39.github.io/ecma402/#sec-canonicalizelocalelist
 */
public abstract class JSToCanonicalizedLocaleListNode extends JavaScriptBaseNode {

    @Child private JSToObjectNode toObjectNode;
    @Child private JSToStringNode toStringNode;
    @Child private JSToLengthNode toLengthNode;
    @Child private TypeOfNode typeOfNode;
    @Child private JSHasPropertyNode hasPropertyNode;
    @Child private PropertyGetNode getLengthPropertyNode;
    @Child private Node foreignGet;
    @Child private JSForeignToJSTypeNode toJSType;

    private final JSContext context;

    protected JSToCanonicalizedLocaleListNode(JSContext context) {
        this.context = context;
    }

    public static JSToCanonicalizedLocaleListNode create(JSContext context) {
        return JSToCanonicalizedLocaleListNodeGen.create(context);
    }

    public abstract String[] executeLanguageTags(Object value);

    protected final JSContext getContext() {
        return context;
    }

    @Specialization()
    protected String[] doRawString(String s) {
        return new String[]{IntlUtil.validateAndCanonicalizeLanguageTag(s)};
    }

    @Specialization(guards = {"isJSString(object)"})
    protected String[] doString(DynamicObject object) {
        String s = JSString.getString(object);
        return doRawString(s);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isForeignObject(object)", "!isString(object)", "!isJSString(object)", "isUndefined(object)"})
    protected String[] doUndefined(DynamicObject object) {
        return new String[]{};
    }

    @Specialization(guards = {"!isForeignObject(object)", "!isString(object)", "!isJSString(object)", "!isUndefined(object)"})
    protected String[] doOtherType(Object object) {

        List<String> result = new ArrayList<>();

        DynamicObject localeObj = (DynamicObject) toObject(object);
        int len = toLength(JSObject.get(localeObj, "length"));
        for (int k = 0; k < len; k++) {
            String pk = toStringVal(k);
            if (JSObject.hasProperty(localeObj, pk)) {
                Object kValue = JSObject.get(localeObj, pk);
                String typeOfKValue = typeOf(kValue);
                if (!typeOfKValue.equals("string") && !typeOfKValue.equals("object")) {
                    throw Errors.createTypeError(Boundaries.stringFormat("String or Object expected in locales list, got %s", typeOfKValue));
                }
                String lt = toStringVal(kValue);
                String canonicalizedLt = IntlUtil.validateAndCanonicalizeLanguageTag(lt);
                if (!Boundaries.listContains(result, canonicalizedLt)) {
                    Boundaries.listAdd(result, canonicalizedLt);
                }
            }
        }
        return result.toArray(new String[]{});
    }

    @Specialization(guards = {"isForeignObject(object)"})
    protected String[] doForeignType(Object object) {

        List<String> result = new ArrayList<>();

        TruffleObject localeObj = toObject(object);
        if (getLengthPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getLengthPropertyNode = insert(PropertyGetNode.create("length", false, context));
        }
        int len = toLength(getLengthPropertyNode.getValue(localeObj));
        for (int k = 0; k < len; k++) {
            String pk = toStringVal(k);
            if (hasProperty(localeObj, pk)) {
                Object kValue = foreignGet(localeObj, pk);
                String typeOfKValue = typeOf(kValue);
                if (!typeOfKValue.equals("string") && !typeOfKValue.equals("object")) {
                    throw Errors.createTypeError(Boundaries.stringFormat("String or Object expected in locales list, got %s", typeOfKValue));
                }
                String lt = toStringVal(kValue);
                String canonicalizedLt = IntlUtil.validateAndCanonicalizeLanguageTag(lt);
                if (!Boundaries.listContains(result, canonicalizedLt)) {
                    Boundaries.listAdd(result, canonicalizedLt);
                }
            }
        }
        return result.toArray(new String[]{});
    }

    protected final boolean hasProperty(TruffleObject object, String key) {
        if (hasPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasPropertyNode = insert(JSHasPropertyNode.create());
        }
        return hasPropertyNode.executeBoolean(object, key);
    }

    private Object foreignGet(TruffleObject thisObj, String key) {
        if (foreignGet == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.foreignGet = insert(Message.READ.createNode());
        }
        try {
            Object foreignResult = ForeignAccess.sendRead(foreignGet, thisObj, key);
            return convertToJSType(foreignResult);
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            return Null.instance;
        }
    }

    private Object convertToJSType(Object foreinResult) {
        if (toJSType == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.toJSType = JSForeignToJSTypeNodeGen.create();
        }
        return toJSType.executeWithTarget(foreinResult);
    }

    private TruffleObject toObject(Object obj) {
        if (toObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
        }
        return toObjectNode.executeTruffleObject(obj);
    }

    private String toStringVal(Object obj) {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toStringNode = insert(JSToStringNode.create());
        }
        return toStringNode.executeString(obj);
    }

    private int toLength(Object obj) {
        if (toLengthNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toLengthNode = insert(JSToLengthNode.create());
        }
        return (int) toLengthNode.executeLong(obj);
    }

    private String typeOf(Object obj) {
        if (typeOfNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeOfNode = insert(TypeOfNode.create());
        }
        return typeOfNode.executeString(obj);
    }
}
