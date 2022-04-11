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
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode.CreateObjectWithPrototypeNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;

// https://tc39.github.io/ecma402/#sec-todatetimeoptions
public abstract class ToDateTimeOptionsNode extends JavaScriptBaseNode {

    private static final String ALL = "all";
    private static final String ANY = "any";
    private static final String DATE = "date";
    private static final String TIME = "time";

    @Child JSToObjectNode toObjectNode;
    final JSContext context;

    public JSContext getContext() {
        return context;
    }

    public ToDateTimeOptionsNode(JSContext context) {
        super();
        this.context = context;
    }

    public abstract JSDynamicObject execute(Object opts, String required, String defaults);

    @SuppressWarnings("unused")
    @Specialization(guards = "isUndefined(opts)")
    public JSDynamicObject fromUndefined(Object opts, String required, String defaults) {
        return setDefaultsIfNeeded(JSOrdinary.createWithNullPrototype(getContext()), required, defaults);
    }

    @Specialization(guards = "!isUndefined(opts)")
    public JSDynamicObject fromOtherThenUndefined(Object opts, String required, String defaults,
                    @Cached("createOrdinaryWithPrototype(context)") CreateObjectWithPrototypeNode createObjectNode) {
        JSDynamicObject options = createObjectNode.execute(toDynamicObject(opts));
        return setDefaultsIfNeeded(options, required, defaults);
    }

    // from step 4 (Let needDefaults be true)
    private static JSDynamicObject setDefaultsIfNeeded(JSDynamicObject options, String required, String defaults) {
        boolean needDefaults = true;
        if (required != null) {
            if (DATE.equals(required) || ANY.equals(required)) {
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, Strings.WEEKDAY));
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, Strings.YEAR));
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, Strings.MONTH));
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, Strings.DAY));
            }
            if (TIME.equals(required) || ANY.equals(required)) {
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, IntlUtil.KEY_DAY_PERIOD));
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, Strings.HOUR));
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, Strings.MINUTE));
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, Strings.SECOND));
                needDefaults &= JSGuards.isUndefined(JSObject.get(options, IntlUtil.KEY_FRACTIONAL_SECOND_DIGITS));
            }
        }
        Object dateStyle = JSObject.get(options, Strings.DATE_STYLE);
        Object timeStyle = JSObject.get(options, Strings.TIME_STYLE);
        if (dateStyle != Undefined.instance || timeStyle != Undefined.instance) {
            needDefaults = false;
        }
        if (DATE.equals(required) && timeStyle != Undefined.instance) {
            throw Errors.createTypeError("timeStyle option is not allowed here");
        }
        if (TIME.equals(required) && dateStyle != Undefined.instance) {
            throw Errors.createTypeError("dateStyle option is not allowed here");
        }
        if (defaults != null) {
            if (needDefaults && (DATE.equals(defaults) || ALL.equals(defaults))) {
                JSRuntime.createDataPropertyOrThrow(options, Strings.YEAR, Strings.NUMERIC);
                JSRuntime.createDataPropertyOrThrow(options, Strings.MONTH, Strings.NUMERIC);
                JSRuntime.createDataPropertyOrThrow(options, Strings.DAY, Strings.NUMERIC);
            }
            if (needDefaults && (TIME.equals(defaults) || ALL.equals(defaults))) {
                JSRuntime.createDataPropertyOrThrow(options, Strings.HOUR, Strings.NUMERIC);
                JSRuntime.createDataPropertyOrThrow(options, Strings.MINUTE, Strings.NUMERIC);
                JSRuntime.createDataPropertyOrThrow(options, Strings.SECOND, Strings.NUMERIC);
            }
        }
        return options;
    }

    private JSDynamicObject toDynamicObject(Object o) {
        if (toObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
        }
        return (JSDynamicObject) toObjectNode.execute(o);
    }
}
