/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
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
    @Specialization(guards = {"!isString(object)", "!isJSString(object)", "isUndefined(object)"})
    protected String[] doUndefined(DynamicObject object) {
        return new String[]{};
    }

    @Specialization(guards = {"!isString(object)", "!isJSString(object)", "!isUndefined(object)"})
    protected String[] doOtherType(Object object) {

        List<String> result = new ArrayList<>();

        DynamicObject localeObj = toObject(object);
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

    private DynamicObject toObject(Object obj) {
        if (toObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
        }
        return (DynamicObject) toObjectNode.executeTruffleObject(obj);
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
