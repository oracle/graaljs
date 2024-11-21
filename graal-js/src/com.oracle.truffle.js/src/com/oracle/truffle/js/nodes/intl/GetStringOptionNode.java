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

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public abstract class GetStringOptionNode extends JavaScriptBaseNode {

    public static final List<String> LOCALE_MATCHER_OPTION_VALUES = List.of(IntlUtil.BEST_FIT, IntlUtil.LOOKUP);
    public static final List<String> HOUR_CYCLE_OPTION_VALUES = List.of(IntlUtil.H11, IntlUtil.H12, IntlUtil.H23, IntlUtil.H24);
    public static final List<String> NARROW_SHORT_LONG_OPTION_VALUES = List.of(IntlUtil.NARROW, IntlUtil.SHORT, IntlUtil.LONG);
    public static final List<String> LONG_SHORT_NARROW_OPTION_VALUES = List.of(IntlUtil.LONG, IntlUtil.SHORT, IntlUtil.NARROW);
    public static final List<String> CASE_FIRST_OPTION_VALUES = List.of(IntlUtil.UPPER, IntlUtil.LOWER, IntlUtil.FALSE);

    private final List<String> validValues;
    private final String fallback;
    @Child PropertyGetNode propertyGetNode;

    protected GetStringOptionNode(JSContext context, TruffleString property, List<String> values, String fallback) {
        this.validValues = values;
        this.fallback = fallback;
        this.propertyGetNode = PropertyGetNode.create(property, false, context);
    }

    public abstract String executeValue(Object options);

    @NeverDefault
    public static GetStringOptionNode create(JSContext context, TruffleString property, List<String> values, String fallback) {
        return GetStringOptionNodeGen.create(context, property, values, fallback);
    }

    @TruffleBoundary
    private void ensureSelectedValueIsValid(String value) {
        if (!validValues.contains(value)) {
            throw Errors.createRangeError(String.format("invalid option %s found where only %s is allowed", value, validValues.toString()));
        }
    }

    @Specialization
    public String getOption(Object options,
                    @Cached JSToStringNode toStringNode,
                    @Cached TruffleString.ToJavaStringNode toJavaStringNode) {
        Object propertyValue = propertyGetNode.getValue(options);
        if (propertyValue == Undefined.instance) {
            return fallback;
        }
        String value = toJavaStringNode.execute(toStringNode.executeString(propertyValue));
        if (validValues != null) {
            ensureSelectedValueIsValid(value);
        }
        return value;
    }

}
