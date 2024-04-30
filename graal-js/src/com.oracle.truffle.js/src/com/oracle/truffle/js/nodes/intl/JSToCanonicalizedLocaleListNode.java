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
package com.oracle.truffle.js.nodes.intl;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocaleObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/**
 * Implementation of ECMA intl402 9.2.1 "CanonicalizeLocaleList" as Truffle node.
 * https://tc39.github.io/ecma402/#sec-canonicalizelocalelist
 */
@ImportStatic({JSConfig.class})
public abstract class JSToCanonicalizedLocaleListNode extends JavaScriptBaseNode {
    final JSContext context;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected JSToCanonicalizedLocaleListNode(JSContext context) {
        this.context = context;
    }

    public static JSToCanonicalizedLocaleListNode create(JSContext context) {
        return JSToCanonicalizedLocaleListNodeGen.create(context);
    }

    public abstract String[] executeLanguageTags(Object value);

    @Specialization
    protected String[] doTString(TruffleString s) {
        return doJavaString(Strings.toJavaString(s));
    }

    @Specialization(guards = {"isUndefined(object)"})
    protected String[] doUndefined(@SuppressWarnings("unused") Object object) {
        return new String[0];
    }

    @Specialization
    protected String[] doLocale(JSLocaleObject object) {
        return doJavaString(object.getInternalState().getLocale());
    }

    @Specialization(guards = {"!isForeignObject(object)", "!isString(object)", "!isUndefined(object)", "!isJSLocale(object)"})
    protected String[] doOtherType(Object object,
                    @Cached JSToObjectNode toObjectNode,
                    @Cached("create(context)") JSGetLengthNode getLengthNode,
                    @Cached JSHasPropertyNode hasPropertyNode,
                    @Cached @Shared("typeOfNode") TypeOfNode typeOfNode,
                    @Cached @Shared("toStringNode") JSToStringNode toStringNode,
                    @Cached @Shared("equalsNode") TruffleString.EqualNode equalsNode,
                    @Cached @Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode) {
        List<String> result = new ArrayList<>();
        JSDynamicObject localeObj = (JSDynamicObject) toObjectNode.execute(object);
        long len = getLengthNode.executeLong(localeObj);
        for (long k = 0; k < len; k++) {
            if (hasPropertyNode.executeBoolean(localeObj, k)) {
                Object kValue = JSObject.get(localeObj, k);
                TruffleString typeOfKValue = typeOfNode.executeString(kValue);
                if (JSRuntime.isNullOrUndefined(kValue) || ((!Strings.equals(equalsNode, Strings.STRING, typeOfKValue) && !Strings.equals(equalsNode, Strings.OBJECT, typeOfKValue)))) {
                    errorBranch.enter();
                    throw Errors.createTypeError(Boundaries.stringFormat("String or Object expected in locales list, got %s", typeOfKValue));
                }
                String lt;
                if (JSLocale.isJSLocale(kValue)) {
                    lt = ((JSLocaleObject) kValue).getInternalState().getLocale();
                } else {
                    lt = Strings.toJavaString(toJavaStringNode, toStringNode.executeString(kValue));
                }
                String canonicalizedLt = IntlUtil.validateAndCanonicalizeLanguageTag(lt);
                if (!Boundaries.listContains(result, canonicalizedLt)) {
                    Boundaries.listAdd(result, canonicalizedLt);
                }
            }
        }
        return result.toArray(new String[]{});
    }

    @Specialization(guards = {"isForeignObject(object)"})
    protected String[] doForeignType(Object object,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary arrayInterop,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary valueInterop,
                    @Cached ImportValueNode importValueNode,
                    @Cached @Shared("typeOfNode") TypeOfNode typeOfNode,
                    @Cached @Shared("toStringNode") JSToStringNode toStringNode,
                    @Cached @Shared("equalsNode") TruffleString.EqualNode equalsNode,
                    @Cached @Shared("toJavaStringNode") TruffleString.ToJavaStringNode toJavaStringNode) {
        List<String> result = new ArrayList<>();
        long len;
        try {
            len = arrayInterop.getArraySize(object);
        } catch (UnsupportedMessageException e) {
            errorBranch.enter();
            throw Errors.createTypeErrorInteropException(object, e, "getArraySize", this);
        }
        for (long k = 0; k < len; k++) {
            if (arrayInterop.isArrayElementReadable(object, k)) {
                String tag;
                try {
                    Object foreignValue = arrayInterop.readArrayElement(object, k);
                    if (valueInterop.isString(foreignValue)) {
                        tag = valueInterop.asString(foreignValue);
                    } else {
                        Object kValue = importValueNode.executeWithTarget(foreignValue);
                        TruffleString typeOfKValue = typeOfNode.executeString(kValue);
                        if (!Strings.equals(equalsNode, Strings.STRING, typeOfKValue) && !Strings.equals(equalsNode, Strings.OBJECT, typeOfKValue)) {
                            errorBranch.enter();
                            throw Errors.createTypeError(Boundaries.stringFormat("String or Object expected in locales list, got %s", typeOfKValue));
                        }
                        tag = Strings.toJavaString(toJavaStringNode, toStringNode.executeString(kValue));
                    }
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorInteropException(object, e, "readArrayElement", k, this);
                }
                String canonicalizedTag = IntlUtil.validateAndCanonicalizeLanguageTag(tag);
                if (!Boundaries.listContains(result, canonicalizedTag)) {
                    Boundaries.listAdd(result, canonicalizedTag);
                }
            }
        }
        return result.toArray(new String[]{});
    }

    private static String[] doJavaString(String s) {
        return new String[]{IntlUtil.validateAndCanonicalizeLanguageTag(s)};
    }
}
